package com.inreq.plugin.models

data class ApiRequest(
    val method: HttpMethod = HttpMethod.GET,
    val path: String = "",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val authToken: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH;
    val hasBody: Boolean get() = this in listOf(POST, PUT, PATCH)
}
