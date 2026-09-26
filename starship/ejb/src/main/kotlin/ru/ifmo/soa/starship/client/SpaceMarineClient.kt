package ru.ifmo.soa.starship.client

import jakarta.enterprise.context.ApplicationScoped
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.exception.SpaceMarineServiceUnavailableException
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private const val HTTP_OK = 200
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404
private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
private val READ_TIMEOUT: Duration = Duration.ofSeconds(5)

/**
 * Обращение к первому сервису и перевод его ответов в понятия нашего домена.
 *
 * Проверка имени хоста не отключается: это распространённый шорткат, превращающий TLS
 * в бутафорию. Вместо этого сертификат первого сервиса лежит в нашем truststore.
 */
@ApplicationScoped
open class SpaceMarineClient {
    private val baseUrl: String = System.getProperty("soa.spacemarine.base-url")
        ?: System.getenv("SOA_SPACEMARINE_BASE_URL")
        ?: "https://helios.cs.ifmo.ru:27443"

    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .sslContext(trustStoreContext())
            .build()
    }

    open fun exists(spaceMarineId: Int): Boolean {
        val response = try {
            http.send(
                HttpRequest.newBuilder(URI.create("$baseUrl/space-marines/$spaceMarineId"))
                    .timeout(READ_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        } catch (e: Exception) {
            throw SpaceMarineServiceUnavailableException(Messages.UPSTREAM_UNAVAILABLE, e)
        }
        return when (val status = response.statusCode()) {
            HTTP_OK -> true
            HTTP_NOT_FOUND -> false
            HTTP_BAD_REQUEST -> throw IllegalStateException(Messages.upstreamRejectedRequest(status))
            else -> throw SpaceMarineServiceUnavailableException(Messages.upstreamUnexpectedStatus(status))
        }
    }

    private fun trustStoreContext(): SSLContext {
        val path = System.getenv("SOA_TRUSTSTORE_PATH") ?: error("Не задан SOA_TRUSTSTORE_PATH")
        val password = System.getenv("SOA_TRUSTSTORE_PASSWORD") ?: error("Не задан SOA_TRUSTSTORE_PASSWORD")
        val trustStore = KeyStore.getInstance("PKCS12").apply {
            FileInputStream(path).use { load(it, password.toCharArray()) }
        }
        val trustManagers = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
            .trustManagers
        return SSLContext.getInstance("TLS").apply { init(null, trustManagers, null) }
    }
}
