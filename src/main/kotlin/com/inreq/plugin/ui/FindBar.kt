package com.inreq.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.DefaultHighlighter

/**
 * Reusable Ctrl+F search bar for JTextArea.
 * Attach it with FindBar.install(textArea, container).
 * Press Ctrl+F to open, Escape to close, Enter for next match.
 */
class FindBar private constructor(
    private val textArea: JTextArea,
    private val container: JPanel
) : JPanel(BorderLayout()) {

    private val searchField = JBTextField().apply {
        emptyText.text = "Find..."
        font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        columns = 20
    }
    private val matchLabel = JLabel("").apply {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        foreground = JBColor.GRAY
    }
    private val highlightPainter = DefaultHighlighter.DefaultHighlightPainter(
        JBColor(Color(255, 230, 130), Color(100, 80, 20))
    )
    private var currentMatch = -1
    private var matchPositions = mutableListOf<IntArray>()

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(4, 8)
        )
        isVisible = false

        val closeBtn = JButton(AllIcons.Actions.Close).apply {
            isFocusPainted = false; isContentAreaFilled = false; isBorderPainted = false
            preferredSize = Dimension(20, 20)
            addActionListener { close() }
        }
        val upBtn = JButton(AllIcons.Actions.PreviousOccurence).apply {
            isFocusPainted = false; isContentAreaFilled = false; isBorderPainted = false
            preferredSize = Dimension(20, 20)
            toolTipText = "Previous (Shift+Enter)"
            addActionListener { prevMatch() }
        }
        val downBtn = JButton(AllIcons.Actions.NextOccurence).apply {
            isFocusPainted = false; isContentAreaFilled = false; isBorderPainted = false
            preferredSize = Dimension(20, 20)
            toolTipText = "Next (Enter)"
            addActionListener { nextMatch() }
        }

        val left = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        left.add(closeBtn)
        left.add(searchField)
        left.add(upBtn)
        left.add(downBtn)
        left.add(matchLabel)

        add(left, BorderLayout.WEST)

        // Live search on typing
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = doSearch()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = doSearch()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = doSearch()
        })

        // Enter = next, Shift+Enter = prev, Escape = close
        searchField.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when {
                    e.keyCode == KeyEvent.VK_ESCAPE -> close()
                    e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown -> prevMatch()
                    e.keyCode == KeyEvent.VK_ENTER -> nextMatch()
                }
            }
        })
    }

    fun open() {
        isVisible = true
        searchField.requestFocusInWindow()
        searchField.selectAll()
        container.revalidate()
    }

    fun close() {
        isVisible = false
        textArea.highlighter.removeAllHighlights()
        matchPositions.clear()
        matchLabel.text = ""
        container.revalidate()
        textArea.requestFocusInWindow()
    }

    private fun doSearch() {
        textArea.highlighter.removeAllHighlights()
        matchPositions.clear()
        currentMatch = -1

        val query = searchField.text
        if (query.isBlank()) {
            matchLabel.text = ""
            return
        }

        val text = textArea.text.lowercase()
        val q = query.lowercase()
        var idx = text.indexOf(q)
        while (idx >= 0) {
            matchPositions.add(intArrayOf(idx, idx + q.length))
            try { textArea.highlighter.addHighlight(idx, idx + q.length, highlightPainter) } catch (_: Exception) {}
            idx = text.indexOf(q, idx + 1)
        }

        matchLabel.text = if (matchPositions.isEmpty()) "No results" else "${matchPositions.size} found"
        if (matchPositions.isNotEmpty()) {
            currentMatch = 0
            scrollToMatch()
        }
    }

    private fun nextMatch() {
        if (matchPositions.isEmpty()) return
        currentMatch = (currentMatch + 1) % matchPositions.size
        scrollToMatch()
    }

    private fun prevMatch() {
        if (matchPositions.isEmpty()) return
        currentMatch = if (currentMatch <= 0) matchPositions.size - 1 else currentMatch - 1
        scrollToMatch()
    }

    private fun scrollToMatch() {
        val pos = matchPositions[currentMatch]
        textArea.caretPosition = pos[0]
        textArea.select(pos[0], pos[1])
        matchLabel.text = "${currentMatch + 1} / ${matchPositions.size}"
    }

    companion object {
        /**
         * Install Ctrl+F search on a JTextArea inside a container panel.
         * Returns the FindBar (already added to container's NORTH).
         */
        fun install(textArea: JTextArea, container: JPanel): FindBar {
            val bar = FindBar(textArea, container)
            container.add(bar, BorderLayout.NORTH)

            // Bind Ctrl+F on the textArea
            val ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
            textArea.getInputMap(JComponent.WHEN_FOCUSED).put(ctrlF, "openFind")
            textArea.actionMap.put("openFind", object : AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) = bar.open()
            })

            return bar
        }
    }
}
