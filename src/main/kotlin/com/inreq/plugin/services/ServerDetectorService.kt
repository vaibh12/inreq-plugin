package com.inreq.plugin.services

import com.inreq.plugin.settings.PluginSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Simple service that provides the base URL from settings.
 * Configure the port in Settings → Tools → InReq.
 * Default: http://localhost:8080
 */
@Service(Service.Level.PROJECT)
class ServerDetectorService(private val project: Project) : Disposable {

    fun getBaseUrl(): String {
        val port = PluginSettings.getInstance(project).port
        return "http://localhost:$port"
    }

    fun getPort(): Int = PluginSettings.getInstance(project).port

    fun getStatusText(): String = "localhost:${getPort()}"

    override fun dispose() {}
}
