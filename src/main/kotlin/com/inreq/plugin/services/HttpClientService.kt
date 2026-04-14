package com.inreq.plugin.services

import com.inreq.plugin.models.ApiRequest
import com.inreq.plugin.models.ApiResponse
import com.inreq.plugin.models.HttpMethod
import com.inreq.plugin.settings.PluginSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Service(Service.Level.PROJECT)
class HttpClientService(private val project: Project) {

    fun execute(request: ApiRequest): ApiResponse {
        val settings = PluginSettings.getInstance(project)
        val detector = project.service<ServerDetectorService>()
        val baseUrl = detector.getBaseUrl().trimEnd('/')
        val path = request.path.let { if (it.startsWith("/")) it else "/$it" }
        val fullUrl = "$baseUrl$path"

        val client = OkHttpClient.Builder()
            .connectTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .followRedirects(true)
            .apply {
                if (!settings.verifySsl) {
                    val tm = object : X509TrustManager {
                        override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
                        override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                    val ctx = SSLContext.getInstance("TLS").apply {
                        init(null, arrayOf<TrustManager>(tm), SecureRandom())
                    }
                    sslSocketFactory(ctx.socketFactory, tm)
                    hostnameVerifier { _, _ -> true }
                }
            }
            .build()

        val builder = Request.Builder().url(fullUrl)

        // Headers
        request.headers.forEach { (k, v) -> if (k.isNotBlank()) builder.header(k, v) }

        // Auth
        request.authToken?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", if (it.startsWith("Bearer ")) it else "Bearer $it")
        }

        // Method + body
        val body = request.body?.toRequestBody("application/json".toMediaType())
        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(body ?: "".toRequestBody(null))
            HttpMethod.PUT -> builder.put(body ?: "".toRequestBody(null))
            HttpMethod.DELETE -> if (body != null) builder.delete(body) else builder.delete()
            HttpMethod.PATCH -> builder.patch(body ?: "".toRequestBody(null))
        }

        val start = System.currentTimeMillis()
        return try {
            val resp = client.newCall(builder.build()).execute()
            val respBody = resp.body?.string() ?: ""
            ApiResponse(
                statusCode = resp.code,
                statusText = resp.message,
                headers = resp.headers.toMultimap().mapValues { it.value.joinToString(", ") },
                body = respBody,
                durationMs = System.currentTimeMillis() - start,
                sizeBytes = respBody.toByteArray().size.toLong()
            )
        } catch (e: IOException) {
            ApiResponse(
                statusCode = 0,
                statusText = "Connection Failed",
                headers = emptyMap(),
                body = "Could not connect to $fullUrl\n\n${e.message}",
                durationMs = System.currentTimeMillis() - start,
                sizeBytes = 0
            )
        }
    }
}
