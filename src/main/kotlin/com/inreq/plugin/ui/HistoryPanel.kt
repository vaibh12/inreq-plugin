package com.inreq.plugin.ui

import com.inreq.plugin.models.ApiRequest
import com.inreq.plugin.services.HistoryService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class HistoryPanel(
    private val project: Project,
    private val onReplay: (ApiRequest) -> Unit
) : JPanel(BorderLayout()) {

    private val listModel = DefaultListModel<HistoryService.Entry>()
    private val list = JBList(listModel)
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // Toolbar: just a Clear button at the top
        val toolbar = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(JBLabel("Click any entry to reload it").apply {
                font = JBUI.Fonts.smallFont(); foreground = JBColor.GRAY
            }, BorderLayout.WEST)
            add(JButton("Clear All").apply {
                addActionListener { project.service<HistoryService>().clear(); refresh() }
            }, BorderLayout.EAST)
        }
        add(toolbar, BorderLayout.NORTH)

        // List with custom renderer
        list.cellRenderer = HistoryCellRenderer()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) list.selectedValue?.let { onReplay(it.request) }
        }
        add(JBScrollPane(list), BorderLayout.CENTER)
    }

    fun refresh() {
        listModel.clear()
        project.service<HistoryService>().getAll().forEach { listModel.addElement(it) }
    }

    private inner class HistoryCellRenderer : ListCellRenderer<HistoryService.Entry> {
        override fun getListCellRendererComponent(
            l: JList<out HistoryService.Entry>, entry: HistoryService.Entry,
            index: Int, isSelected: Boolean, hasFocus: Boolean
        ): Component {
            val panel = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                border = JBUI.Borders.empty(6, 10)
                isOpaque = true
                background = if (isSelected) l.selectionBackground else l.background
            }

            // Method label with color
            val methodColor = when (entry.request.method.name) {
                "GET" -> JBColor(Color(99, 153, 34), Color(120, 200, 80))
                "POST" -> JBColor(Color(55, 138, 221), Color(90, 170, 240))
                "PUT" -> JBColor(Color(186, 117, 23), Color(210, 160, 50))
                "DELETE" -> JBColor(Color(200, 60, 60), Color(230, 110, 110))
                "PATCH" -> JBColor(Color(131, 74, 183), Color(170, 120, 220))
                else -> JBColor.GRAY
            }
            val methodLabel = JBLabel(entry.request.method.name).apply {
                font = Font(Font.MONOSPACED, Font.BOLD, 12)
                foreground = methodColor
                preferredSize = JBUI.size(48, 16)
            }

            // Path
            val pathLabel = JBLabel(entry.request.path.take(40)).apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, 11)
                foreground = if (isSelected) l.selectionForeground else JBColor.foreground()
            }

            // Status code + time
            val statusCode = entry.response?.statusCode ?: 0
            val statusColor = when {
                statusCode in 200..299 -> JBColor(Color(99, 153, 34), Color(120, 200, 80))
                statusCode in 400..499 -> JBColor(Color(186, 117, 23), Color(210, 160, 50))
                statusCode >= 500 -> JBColor(Color(200, 60, 60), Color(230, 110, 110))
                else -> JBColor.GRAY
            }
            val rightText = "${if (statusCode > 0) "$statusCode" else "ERR"}  ${timeFmt.format(Date(entry.timestamp))}"
            val rightLabel = JBLabel(rightText).apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, 10)
                foreground = if (isSelected) l.selectionForeground else statusColor
            }

            val left = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
                isOpaque = false
                add(methodLabel, BorderLayout.WEST)
                add(pathLabel, BorderLayout.CENTER)
            }
            panel.add(left, BorderLayout.CENTER)
            panel.add(rightLabel, BorderLayout.EAST)
            return panel
        }
    }
}
