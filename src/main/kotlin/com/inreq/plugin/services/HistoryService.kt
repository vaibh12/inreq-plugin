package com.inreq.plugin.services

import com.inreq.plugin.models.ApiRequest
import com.inreq.plugin.models.ApiResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class HistoryService(private val project: Project) {

    data class Entry(val request: ApiRequest, val response: ApiResponse?, val timestamp: Long = System.currentTimeMillis())

    private val entries = mutableListOf<Entry>()

    fun add(request: ApiRequest, response: ApiResponse?) {
        entries.add(0, Entry(request, response))
        if (entries.size > 50) entries.removeLast()
    }

    fun getAll(): List<Entry> = entries.toList()
    fun clear() = entries.clear()
}
