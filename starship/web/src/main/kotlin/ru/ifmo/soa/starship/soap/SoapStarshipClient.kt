package ru.ifmo.soa.starship.soap

import jakarta.xml.ws.WebServiceException
import org.apache.cxf.configuration.jsse.TLSClientParameters
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import ru.ifmo.soa.starship.api.StarshipRemote
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.query.StarshipQuery
import javax.net.ssl.TrustManager

class SoapTransportException(cause: Throwable) : RuntimeException("Сервис Starship SOAP недоступен, повторите запрос позже", cause)

/** A request owns its proxy/conduit. No mutable JAX-WS request context is shared. */
class SoapStarshipClient(
    private val endpoint: String,
    private val trustManagers: Array<TrustManager>?,
) : StarshipRemote {
    override fun list(query: StarshipQuery): StarshipPageDto = invoke { port ->
        val request = ShipQuery().apply {
            id = query.filter.id
            name = query.filter.name
            page = query.page
            size = query.size
            sort = query.sort.map { value -> ShipSort().apply {
                field = SortField.valueOf(value.field.name)
                descending = value.descending
            } }
        }
        port.list(request).let { StarshipPageDto(it.items.map(Ship::toDto), it.page, it.size, it.totalElements, it.totalPages) }
    }
    override fun getById(id: Long) = invoke { it.getById(id).toDto() }
    override fun create(name: String?) = invoke { it.create(name).toDto() }
    override fun createWithId(id: Long, name: String?) = invoke { it.createWithId(id, name).toDto() }
    override fun rename(id: Long, name: String?) = invoke { it.rename(id, name).toDto() }
    override fun delete(id: Long) = invoke { it.delete(id) }
    override fun board(starshipId: Long, spaceMarineId: Int) = invoke { it.board(starshipId, spaceMarineId).toDto() }
    override fun unload(starshipId: Long, spaceMarineId: Int) = invoke { it.unload(starshipId, spaceMarineId).toDto() }

    private fun <T> invoke(operation: (StarshipPort) -> T): T {
        // Build from the shared SEI: no startup WSDL HTTP request (or deployment-order dependency).
        val factory = JaxWsProxyFactoryBean().apply {
            serviceClass = StarshipPort::class.java
            address = endpoint
            properties = mapOf("force.urlconnection.http.conduit" to true)
        }
        val port = factory.create() as StarshipPort
        val client = ClientProxy.getClient(port)
        try {
            val conduit = client.conduit as HTTPConduit
            conduit.client = HTTPClientPolicy().apply {
                connectionTimeout = 5_000
                receiveTimeout = 30_000
                isAutoRedirect = false
                maxRetransmits = 0
            }
            conduit.tlsClientParameters = TLSClientParameters().apply {
                trustManagers = this@SoapStarshipClient.trustManagers
                isDisableCNCheck = false
            }
            return operation(port)
        } catch (e: StarshipFault) {
            throw e
        } catch (e: WebServiceException) {
            throw SoapTransportException(e)
        } finally {
            client.destroy()
        }
    }
}

private fun Ship.toDto() = StarshipDto(id, requireNotNull(name), marines.toList())
