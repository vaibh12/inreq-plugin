package com.inreq.plugin.models

data class ApiResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, String>,
    val body: String,
    val durationMs: Long,
    val sizeBytes: Long
) {
    val isSuccess: Boolean get() = statusCode in 200..299

    val formattedSize: String get() = when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1048576 -> "${sizeBytes / 1024} KB"
        else -> "${"%.1f".format(sizeBytes / 1048576.0)} MB"
    }
}
