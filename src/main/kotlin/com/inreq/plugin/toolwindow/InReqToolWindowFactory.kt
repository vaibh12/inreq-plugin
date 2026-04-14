package com.inreq.plugin.toolwindow

import com.inreq.plugin.services.HistoryService
import com.inreq.plugin.services.HttpClientService
import com.inreq.plugin.services.ServerDetectorService
import com.inreq.plugin.ui.HistoryPanel
import com.inreq.plugin.ui.RequestPanel
import com.inreq.plugin.ui.ResponsePanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class InReqToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        private val panels = ConcurrentHashMap<Project, InReqPanel>()
        fun getPanel(project: Project): InReqPanel? = panels[project]
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = InReqPanel(project)
        panels[project] = panel
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )
    }
}

class InReqPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val requestPanel: RequestPanel = RequestPanel(project)
    private val responsePanel: ResponsePanel = ResponsePanel(project)
    private val outputTabs: JBTabbedPane
    private val historyPanel: HistoryPanel
    private val split: JSplitPane

    // Remember the divider position before collapse
    private var expandedDividerLocation = -1

    init {
        historyPanel = HistoryPanel(project) { req ->
            requestPanel.loadRequest(req)
            outputTabs.selectedIndex = 0
        }

        outputTabs = JBTabbedPane(SwingConstants.TOP).apply {
            font = JBUI.Fonts.label()
            addTab("Response", responsePanel)
            addTab("History", historyPanel)
        }

        split = JSplitPane(JSplitPane.VERTICAL_SPLIT, requestPanel, outputTabs).apply {
            dividerSize = JBUI.scale(4)
            resizeWeight = 0.45
        }
        add(split, BorderLayout.CENTER)

        // Wire send
        requestPanel.sendButton.addActionListener { sendRequest() }
        requestPanel.pathField.addActionListener { sendRequest() }
    }

    fun sendRequest() {
        val req = requestPanel.buildRequest()
        if (req.path.isBlank()) return

        requestPanel.refreshStatus()
        val base = project.service<ServerDetectorService>().getBaseUrl().trimEnd('/')
        val path = req.path.let { if (it.startsWith("/")) it else "/$it" }
        val url = "$base$path"

        // Collapse request section to give more room to response
        collapseRequest()

        outputTabs.selectedIndex = 0
        responsePanel.showLoading(url)

        ApplicationManager.getApplication().executeOnPooledThread {
            val resp = project.service<HttpClientService>().execute(req)
            project.service<HistoryService>().add(req, resp)

            SwingUtilities.invokeLater {
                responsePanel.showResponse(resp, url)
                historyPanel.refresh()
                requestPanel.refreshStatus()
            }
        }
    }

    /** Collapse request panel — keep URL bar visible, hide tabs */
    private fun collapseRequest() {
        // Remember current position so we can restore
        expandedDividerLocation = split.dividerLocation
        // Collapse: hide the request tabs, shrink the top panel
        requestPanel.collapseTabs()
        // Move divider up to show only URL bar + status
        SwingUtilities.invokeLater {
            split.dividerLocation = JBUI.scale(100)
        }
    }

    /** Expand request panel back — called when user clicks a request tab */
    fun expandRequest() {
        requestPanel.expandTabs()
        SwingUtilities.invokeLater {
            split.dividerLocation = if (expandedDividerLocation > 0)
                expandedDividerLocation
            else
                (split.height * 0.45).toInt()
        }
    }
}
