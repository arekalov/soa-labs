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
    fun exists(spaceMarineId: Int): Boolean = try {
        client.get()
            .uri("/space-marines/{id}", spaceMarineId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange { _, response ->
                when (val status = response.statusCode.value()) {
                    HTTP_OK -> true
                    HTTP_NOT_FOUND -> false
                    HTTP_BAD_REQUEST -> throw IllegalStateException(Messages.upstreamRejectedRequest(status))
                    else -> throw SpaceMarineServiceUnavailableException(Messages.upstreamUnexpectedStatus(status))
                }
            }
    } catch (e: ResourceAccessException) {
        throw SpaceMarineServiceUnavailableException(Messages.UPSTREAM_UNAVAILABLE, e)
    }
}
