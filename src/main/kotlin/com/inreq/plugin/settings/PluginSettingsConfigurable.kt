package com.inreq.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class PluginSettingsConfigurable(private val project: Project) : Configurable {

    private var panel: JPanel? = null
    private val timeoutSpinner = JSpinner(SpinnerNumberModel(30, 1, 300, 5))
    private val sslCheckbox = JBCheckBox("Verify SSL certificates")
    private val portSpinner = JSpinner(SpinnerNumberModel(8080, 1, 65535, 1))

    override fun getDisplayName(): String = "InReq"

    override fun createComponent(): JComponent {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Request timeout (seconds):", timeoutSpinner)
            .addComponent(sslCheckbox)
            .addLabeledComponent("Server port (your application runs on this):", portSpinner)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val s = PluginSettings.getInstance(project)
        return timeoutSpinner.value as Int != s.timeoutSeconds
                || sslCheckbox.isSelected != s.verifySsl
                || portSpinner.value as Int != s.port
    }

    override fun apply() {
        val s = PluginSettings.getInstance(project)
        s.timeoutSeconds = timeoutSpinner.value as Int
        s.verifySsl = sslCheckbox.isSelected
        s.port = portSpinner.value as Int
    }

    override fun reset() {
        val s = PluginSettings.getInstance(project)
        timeoutSpinner.value = s.timeoutSeconds
        sslCheckbox.isSelected = s.verifySsl
        portSpinner.value = s.port
    }
}
