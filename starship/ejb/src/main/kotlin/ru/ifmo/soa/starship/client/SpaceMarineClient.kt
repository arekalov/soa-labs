package ru.ifmo.soa.starship.client

import jakarta.enterprise.context.ApplicationScoped
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.exception.SpaceMarineServiceUnavailableException
import java.io.FileInputStream
import java.net.URI
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private const val HTTP_OK = 200
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404
private const val CONNECT_TIMEOUT_MILLIS = 3_000
private const val READ_TIMEOUT_MILLIS = 5_000

/**
 * Межсервисный вызов ЛР4: только через Mule ESB, без обходного пути к SpaceMarine.
 * Шина сохраняет HTTP-статусы первого сервиса; бизнес-смысл ответа определяет Starship.
 * При недоступности Mule операция возвращает 503, а не обращается к сервису напрямую.
 */
@ApplicationScoped
open class SpaceMarineClient {
    private val muleUrl: String = System.getProperty("soa.mule.url")
        ?: System.getenv("SOA_MULE_URL")
        ?: "http://127.0.0.1:27780"

    private val tls by lazy { trustStoreContext().socketFactory }

    open fun exists(spaceMarineId: Int): Boolean {
        // Вызов синхронный, выполняется SOAP worker. URLConnection не заводит отдельные
        // selector/executor-потоки на каждом узле в тесном лимите helios.
        val status = try {
            val connection = URI.create("${muleUrl.trimEnd('/')}/space-marines/$spaceMarineId")
                .toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                if (connection is HttpsURLConnection) connection.sslSocketFactory = tls
                val code = connection.responseCode
                (if (code >= 400) connection.errorStream else connection.inputStream)?.close()
                code
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            throw SpaceMarineServiceUnavailableException(Messages.UPSTREAM_UNAVAILABLE, e)
        }
        return when (status) {
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
