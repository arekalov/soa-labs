package ru.ifmo.soa.starship.adapter.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import ru.ifmo.soa.starship.application.error.SpaceMarineServiceUnavailableException
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * Настройка защищённого канала к первому сервису.
 *
 * Проверка имени хоста намеренно **не отключается**: это распространённый шорткат,
 * который превращает TLS в бутафорию. Вместо этого сертификат первого сервиса лежит
 * в нашем truststore, а в его SAN входят все имена, под которыми к сервису обращаются,
 * включая `localhost` и `127.0.0.1` для доступа через SSH-туннель.
 */
@Configuration
class SpaceMarineClientConfig {

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

        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(READ_TIMEOUT)
        }

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    private companion object {
        const val TRUSTSTORE_TYPE = "PKCS12"

        /** Таймауты короткие: лучше честный 503, чем повисший запрос на защите. */
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
        val READ_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}

/**
 * Обращение к первому сервису и перевод его ответов в понятия нашего домена.
 */
@Component
class RestSpaceMarineGateway(
    private val client: RestClient,
) : SpaceMarineGateway {

    override fun exists(spaceMarineId: Int): Boolean = try {
        client.get()
            .uri("/space-marines/{id}", spaceMarineId)
            .accept(MediaType.APPLICATION_JSON)
            // exchange, а не retrieve: на 404 первый сервис отдаёт тело схемы Error,
            // и retrieve попытался бы разобрать его как полезную нагрузку.
            .exchange { _, response ->
                when (val status = response.statusCode.value()) {
                    HTTP_OK -> true
                    HTTP_NOT_FOUND -> false

                    // 400 здесь означает наш баг: идентификатор уже проверен на > 0.
                    // Наружу это должно уйти как 500, а не как ошибка клиента.
                    HTTP_BAD_REQUEST -> throw IllegalStateException(
                        "Первый сервис отклонил корректный запрос: $status",
                    )

                    else -> throw SpaceMarineServiceUnavailableException(
                        "Сервис SpaceMarine вернул неожиданный статус $status",
                    )
                }
            }
    } catch (e: ResourceAccessException) {
        // Отказ соединения, таймаут, сбой рукопожатия TLS, неразрешимое имя —
        // всё это для вызывающего одно и то же: сервис недоступен.
        throw SpaceMarineServiceUnavailableException(
            "Сервис SpaceMarine недоступен, повторите запрос позже",
            e,
        )
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
    }
}
