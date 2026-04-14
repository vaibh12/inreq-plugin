package com.inreq.plugin.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(name = "InReqSettings", storages = [Storage("inreqSettings.xml")])
class PluginSettings : PersistentStateComponent<PluginSettings> {

    var timeoutSeconds: Int = 30
    var verifySsl: Boolean = false   // off by default for local dev
    var port: Int = 8080             // your server port — change in Settings → Tools → InReq

    override fun getState(): PluginSettings = this
    override fun loadState(state: PluginSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        fun getInstance(project: Project): PluginSettings = project.service()
    }
}
