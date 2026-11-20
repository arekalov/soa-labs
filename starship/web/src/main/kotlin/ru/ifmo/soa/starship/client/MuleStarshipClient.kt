package ru.ifmo.soa.starship.client

import tools.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class MuleUnavailableException(cause: Exception) : RuntimeException("Сервис Starship недоступен, повторите запрос позже", cause)

/** JSON transport only. Mule owns SOAP, response mapping and business error statuses. */
class MuleStarshipClient(
    private val endpoint: String,
    private val sslSocketFactory: SSLSocketFactory,
    private val mapper: ObjectMapper,
) {
    fun call(operation: String, parameters: Any): ResponseEntity<ByteArray> {
        try {
            val connection = URI.create("${endpoint.trimEnd('/')}/$operation").toURL().openConnection() as HttpsURLConnection
            try {
                connection.sslSocketFactory = sslSocketFactory
                connection.connectTimeout = 5_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                val request = mapper.writeValueAsBytes(parameters)
                connection.setFixedLengthStreamingMode(request.size)
                connection.outputStream.use { it.write(request) }
                val status = connection.responseCode
                val body = (if (status >= 400) connection.errorStream else connection.inputStream)?.use { it.readBytes() }
                if (status == 204) return ResponseEntity.noContent().build()
                check(status in 200..599 && connection.contentType?.startsWith("application/json") == true) {
                    "Invalid Mule response"
                }
                return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            throw MuleUnavailableException(e)
        }
    }
}
