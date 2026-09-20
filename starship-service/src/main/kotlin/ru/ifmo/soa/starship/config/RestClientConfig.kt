package ru.ifmo.soa.starship.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private const val TRUSTSTORE_TYPE = "PKCS12"

/** Таймауты короткие: лучше честный 503, чем повисший запрос на защите. */
private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
private val READ_TIMEOUT: Duration = Duration.ofSeconds(5)

/**
 * Защищённый канал к первому сервису.
 *
 * Проверка имени хоста намеренно не отключается: это распространённый шорткат, который
 * превращает TLS в бутафорию. Вместо этого сертификат первого сервиса лежит в нашем
 * truststore, а в его SAN входят все имена, под которыми к сервису обращаются.
 */
@Configuration
class RestClientConfig {

    @Bean
    fun soaSslContext(
        @Value("\${soa.truststore.path}") trustStorePath: String,
        @Value("\${soa.truststore.password}") trustStorePassword: String,
    ): SSLContext {
        val trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE)
        Files.newInputStream(Path.of(trustStorePath)).use { input ->
            trustStore.load(input, trustStorePassword.toCharArray())
        }

        val trustManagers = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
            .trustManagers

        return SSLContext.getInstance("TLS").apply {
            // Ключей не даём: клиентская аутентификация спецификацией не требуется.
            init(null, trustManagers, null)
        }
    }

    @Bean
    fun spaceMarineRestClient(
        sslContext: SSLContext,
        @Value("\${soa.spacemarine.base-url}") baseUrl: String,
    ): RestClient {
        val httpClient = HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(CONNECT_TIMEOUT)
            .build()

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(READ_TIMEOUT) })
            .build()
    }
}
