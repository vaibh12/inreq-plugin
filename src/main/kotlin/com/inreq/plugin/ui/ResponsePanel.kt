package com.inreq.plugin.ui

import com.inreq.plugin.models.ApiResponse
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

class ResponsePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val statusLabel = JBLabel("").apply {
        font = JBUI.Fonts.label().asBold()
        foreground = JBColor.GRAY
    }
    private val statusTextLabel = JBLabel("").apply {
        font = JBUI.Fonts.smallFont()
        foreground = JBColor.GRAY
    }
    private val metaLabel = JBLabel("").apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        foreground = JBColor.GRAY
    }

    private val bodyArea = JTextArea().apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(8)
        text = "Send a request to see the response here."
        foreground = JBColor.GRAY
    }

    // The body container that FindBar attaches to
    private val bodyContainer = JPanel(BorderLayout())

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    init {
        // Status bar
        val bar = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 10)
            val left = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
            left.add(statusLabel)
            left.add(statusTextLabel)
            add(left, BorderLayout.WEST)
            add(metaLabel, BorderLayout.EAST)
        }
        add(bar, BorderLayout.NORTH)

        // Body with Ctrl+F search
        bodyContainer.add(JBScrollPane(bodyArea).apply {
            border = BorderFactory.createEmptyBorder()
        }, BorderLayout.CENTER)
        FindBar.install(bodyArea, bodyContainer)

        add(bodyContainer, BorderLayout.CENTER)
    }

    fun showLoading(url: String) {
        statusLabel.text = ""
        statusLabel.isOpaque = false
        statusTextLabel.text = "Sending..."
        statusTextLabel.foreground = JBColor.GRAY
        metaLabel.text = ""
        bodyArea.text = "Sending request to $url ..."
        bodyArea.foreground = JBColor.GRAY
    }

    fun showResponse(resp: ApiResponse, url: String) {
        // Status code pill
        statusLabel.text = " ${resp.statusCode} "
        statusLabel.isOpaque = true
        statusLabel.foreground = Color.WHITE
        statusLabel.background = when {
            resp.statusCode == 0 -> JBColor.RED
            resp.isSuccess -> JBColor(Color(99, 153, 34), Color(80, 160, 60))
            resp.statusCode in 400..499 -> JBColor(Color(186, 117, 23), Color(200, 150, 40))
            else -> JBColor(Color(226, 75, 74), Color(220, 100, 100))
        }

        // Status text
        statusTextLabel.text = resp.statusText
        statusTextLabel.foreground = UIManager.getColor("Label.foreground") ?: JBColor.foreground()

        // Meta
        metaLabel.text = "${resp.durationMs}ms  |  ${resp.formattedSize}"

        // Body — pretty print JSON
        bodyArea.foreground = UIManager.getColor("TextArea.foreground") ?: JBColor.foreground()
        bodyArea.text = try {
            prettyGson.toJson(JsonParser.parseString(resp.body))
        } catch (_: Exception) { resp.body }
        bodyArea.caretPosition = 0
    }
}
