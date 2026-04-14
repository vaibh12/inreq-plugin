package com.inreq.plugin.ui

import com.inreq.plugin.models.ApiRequest
import com.inreq.plugin.models.HttpMethod
import com.inreq.plugin.services.ServerDetectorService
import com.inreq.plugin.settings.PluginSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

class RequestPanel(private val project: Project) : JPanel(BorderLayout()) {

    // --- Status ---
    private val statusDot = JPanel().apply {
        preferredSize = Dimension(8, 8); maximumSize = Dimension(8, 8); isOpaque = true
    }
    private val statusText = JBLabel("").apply { font = JBUI.Fonts.smallFont().asBold() }
    private val statusMode = JBLabel("").apply { font = JBUI.Fonts.miniFont(); foreground = JBColor.GRAY }

    // --- URL bar ---
    val methodBox = ComboBox(HttpMethod.entries.toTypedArray()).apply {
        preferredSize = JBUI.size(80, 30)
    }
    val pathField = JBTextField().apply {
        emptyText.text = "v1/policy/list"
        font = Font(Font.MONOSPACED, Font.PLAIN, 13)
    }
    val sendButton = JButton("Send", AllIcons.Actions.Execute).apply {
        font = JBUI.Fonts.label().asBold()
    }
    val reloadButton = JButton(AllIcons.Actions.Refresh).apply {
        toolTipText = "Show request tabs"
        preferredSize = JBUI.size(30, 30)
        isFocusPainted = false
        addActionListener { expandTabs() }
    }
    private val resolvedLabel = JBLabel("").apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 10); foreground = JBColor.GRAY
    }

    // --- Headers table ---
    val headersModel = DefaultTableModel(arrayOf("Header", "Value"), 0).apply {
        addRow(arrayOf("Content-Type", "application/json"))
    }
    private val headersTable = JBTable(headersModel).apply {
        rowHeight = JBUI.scale(26); setShowGrid(true); gridColor = JBColor.border()
    }

    // --- Auth ---
    val authField = JBTextField().apply {
        emptyText.text = "Bearer eyJhbGciOi..."
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    // --- Body (with Ctrl+F) ---
    val bodyArea = JTextArea(5, 1).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        lineWrap = true; wrapStyleWord = true; tabSize = 2
        border = JBUI.Borders.empty(6)
    }
    private val bodyContainer = JPanel(BorderLayout())

    // --- Params table ---
    val paramsModel = DefaultTableModel(arrayOf("Param", "Value"), 0)
    private val paramsTable = JBTable(paramsModel).apply {
        rowHeight = JBUI.scale(26); setShowGrid(true); gridColor = JBColor.border()
    }

    // Tabs section — can be collapsed
    private lateinit var tabs: JBTabbedPane
    lateinit var tabsWrapper: JPanel
        private set

    init {
        buildUI()
        refreshStatus()

        // Live-update resolved URL
        pathField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateResolved()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateResolved()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateResolved()
        })
    }

    private fun buildUI() {
        // === Top: always visible (status + url bar + resolved) ===
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        // Status strip
        val strip = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(4, 8)
            )
            preferredSize = Dimension(0, JBUI.scale(28))
            val left = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
            left.add(statusDot); left.add(statusText)
            add(left, BorderLayout.WEST); add(statusMode, BorderLayout.EAST)
        }
        topPanel.add(strip)

        // URL bar
        val urlBar = JPanel(BorderLayout(JBUI.scale(4), 0)).apply {
            border = JBUI.Borders.empty(8, 8, 4, 8)
            add(methodBox, BorderLayout.WEST)
            add(pathField, BorderLayout.CENTER)
            val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(2), 0))
            buttons.add(reloadButton)
            buttons.add(sendButton)
            add(buttons, BorderLayout.EAST)
        }
        topPanel.add(urlBar)

        // Resolved URL
        val resolvedRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty(0, 8, 4, 8)
        }
        resolvedRow.add(resolvedLabel)
        topPanel.add(resolvedRow)

        add(topPanel, BorderLayout.NORTH)

        // === Tabs: collapsible section ===
        tabs = JBTabbedPane(SwingConstants.TOP).apply {
            font = JBUI.Fonts.label()

            // Headers
            addTab("Headers (${headersModel.rowCount})",
                ToolbarDecorator.createDecorator(headersTable)
                    .setAddAction { headersModel.addRow(arrayOf("", "")) }
                    .setRemoveAction { headersTable.selectedRow.takeIf { it >= 0 }?.let { headersModel.removeRow(it) } }
                    .createPanel()
            )

            // Auth
            val authPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(10)
                add(JBLabel("Bearer Token").apply {
                    font = JBUI.Fonts.smallFont(); foreground = JBColor.GRAY
                }, BorderLayout.NORTH)
                add(Box.createVerticalStrut(JBUI.scale(6)))
                add(authField, BorderLayout.CENTER)
            }
            addTab("Auth", authPanel)

            // Body with Ctrl+F
            bodyContainer.apply {
                border = JBUI.Borders.empty(4)
                add(JScrollPane(bodyArea), BorderLayout.CENTER)
            }
            FindBar.install(bodyArea, bodyContainer)
            addTab("Body", bodyContainer)

            // Params
            addTab("Params (${paramsModel.rowCount})",
                ToolbarDecorator.createDecorator(paramsTable)
                    .setAddAction { paramsModel.addRow(arrayOf("", "")) }
                    .setRemoveAction { paramsTable.selectedRow.takeIf { it >= 0 }?.let { paramsModel.removeRow(it) } }
                    .createPanel()
            )
        }

        // Update tab titles
        headersModel.addTableModelListener { tabs.setTitleAt(0, "Headers (${headersModel.rowCount})") }
        paramsModel.addTableModelListener { tabs.setTitleAt(3, "Params (${paramsModel.rowCount})") }

        // Clicking a tab expands the section if collapsed
        tabs.addChangeListener { expandTabs() }

        tabsWrapper = JPanel(BorderLayout()).apply { add(tabs, BorderLayout.CENTER) }
        add(tabsWrapper, BorderLayout.CENTER)
    }

    /** Collapse the tabs section (called on Send) */
    fun collapseTabs() {
        tabsWrapper.isVisible = false
        revalidate(); repaint()
    }

    /** Expand the tabs section */
    fun expandTabs() {
        if (!tabsWrapper.isVisible) {
            tabsWrapper.isVisible = true
            revalidate(); repaint()
        }
    }

    fun refreshStatus() {
        val detector = project.service<ServerDetectorService>()
        statusDot.background = JBColor(Color(99, 153, 34), Color(120, 200, 80))
        statusText.text = detector.getStatusText()
        statusText.foreground = JBColor(Color(99, 153, 34), Color(120, 200, 80))
        statusMode.text = "port: ${detector.getPort()}"
        updateResolved()
    }

    private fun updateResolved() {
        val base = project.service<ServerDetectorService>().getBaseUrl().trimEnd('/')
        val path = pathField.text.trim().let { if (it.isEmpty()) "" else if (it.startsWith("/")) it else "/$it" }
        resolvedLabel.text = "$base$path${buildQueryString()}"
    }

    private fun buildQueryString(): String {
        val pairs = mutableListOf<String>()
        for (i in 0 until paramsModel.rowCount) {
            val k = paramsModel.getValueAt(i, 0)?.toString()?.trim() ?: ""
            val v = paramsModel.getValueAt(i, 1)?.toString()?.trim() ?: ""
            if (k.isNotEmpty()) pairs.add("$k=$v")
        }
        return if (pairs.isEmpty()) "" else "?${pairs.joinToString("&")}"
    }

    private fun readHeaders(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until headersModel.rowCount) {
            val k = headersModel.getValueAt(i, 0)?.toString()?.trim() ?: ""
            val v = headersModel.getValueAt(i, 1)?.toString()?.trim() ?: ""
            if (k.isNotEmpty()) map[k] = v
        }
        return map
    }

    fun buildRequest(): ApiRequest {
        return ApiRequest(
            method = methodBox.selectedItem as HttpMethod,
            path = pathField.text.trim() + buildQueryString(),
            headers = readHeaders(),
            body = bodyArea.text.takeIf { it.isNotBlank() },
            authToken = authField.text.takeIf { it.isNotBlank() }
        )
    }

    fun loadRequest(req: ApiRequest) {
        val parts = req.path.split("?", limit = 2)
        methodBox.selectedItem = req.method
        pathField.text = parts[0]

        while (paramsModel.rowCount > 0) paramsModel.removeRow(0)
        if (parts.size > 1) {
            parts[1].split("&").forEach { pair ->
                val kv = pair.split("=", limit = 2)
                if (kv.isNotEmpty()) paramsModel.addRow(arrayOf(kv[0], kv.getOrElse(1) { "" }))
            }
        }

        while (headersModel.rowCount > 0) headersModel.removeRow(0)
        req.headers.forEach { (k, v) -> headersModel.addRow(arrayOf(k, v)) }
        if (headersModel.rowCount == 0) headersModel.addRow(arrayOf("Content-Type", "application/json"))

        authField.text = req.authToken ?: ""
        bodyArea.text = req.body ?: ""
        expandTabs()
        updateResolved()
    }
}
