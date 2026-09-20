package ru.ifmo.soa.starship.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.exception.SpaceMarineServiceUnavailableException

private const val HTTP_OK = 200
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404

/** Обращение к первому сервису и перевод его ответов в понятия нашего домена. */
@Component
class SpaceMarineClient(
    private val client: RestClient,
) {

    /** @return `true`, если десантник существует в первом сервисе. */
    fun exists(spaceMarineId: Int): Boolean = try {
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
                    HTTP_BAD_REQUEST -> throw IllegalStateException(Messages.upstreamRejectedRequest(status))
                    else -> throw SpaceMarineServiceUnavailableException(Messages.upstreamUnexpectedStatus(status))
                }
            }
    } catch (e: ResourceAccessException) {
        // Отказ соединения, таймаут, сбой рукопожатия TLS, неразрешимое имя —
        // для вызывающего это одно и то же.
        throw SpaceMarineServiceUnavailableException(Messages.UPSTREAM_UNAVAILABLE, e)
    }
}
