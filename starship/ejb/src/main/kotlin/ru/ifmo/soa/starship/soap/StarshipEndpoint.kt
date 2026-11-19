package ru.ifmo.soa.starship.soap

import jakarta.ejb.EJB
import jakarta.ejb.Stateless
import jakarta.ejb.TransactionAttribute
import jakarta.ejb.TransactionAttributeType
import jakarta.jws.WebService
import ru.ifmo.soa.starship.Messages
import ru.ifmo.soa.starship.api.StarshipRemote
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.exception.*
import ru.ifmo.soa.starship.query.*
import java.util.logging.Level
import java.util.logging.Logger

/** SOAP-адаптер: транзакции и бизнес-правила остаются в StarshipBean. */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@WebService(
    endpointInterface = "ru.ifmo.soa.starship.soap.StarshipPort",
    serviceName = "StarshipService",
    portName = "StarshipPort",
    targetNamespace = StarshipPort.NS,
)
class StarshipEndpoint : StarshipPort {
    @EJB(lookup = "java:global/starship/starship-ejb/StarshipBean!ru.ifmo.soa.starship.api.StarshipRemote")
    private lateinit var business: StarshipRemote

    @Throws(StarshipFault::class)
    override fun list(query: ShipQuery?): ShipPage = call {
        val result = business.list(toQuery(query))
        ShipPage().apply {
            items = result.items.map(::ship)
            page = result.page
            size = result.size
            totalElements = result.totalElements
            totalPages = result.totalPages
        }
    }

    @Throws(StarshipFault::class)
    override fun getById(id: Long): Ship = call { ship(business.getById(id)) }

    @Throws(StarshipFault::class)
    override fun create(name: String?): Ship = call { ship(business.create(name)) }

    @Throws(StarshipFault::class)
    override fun createWithId(id: Long, name: String?): Ship = call { ship(business.createWithId(id, name)) }

    @Throws(StarshipFault::class)
    override fun rename(id: Long, name: String?): Ship = call { ship(business.rename(id, name)) }

    @Throws(StarshipFault::class)
    override fun delete(id: Long): Unit = call { business.delete(id) }

    @Throws(StarshipFault::class)
    override fun board(starshipId: Long, spaceMarineId: Int): Ship = call { ship(business.board(starshipId, spaceMarineId)) }

    @Throws(StarshipFault::class)
    override fun unload(starshipId: Long, spaceMarineId: Int): Ship = call { ship(business.unload(starshipId, spaceMarineId)) }

    private fun ship(dto: StarshipDto) = Ship().apply {
        id = dto.id
        name = dto.name
        marines = dto.marines
    }

    // Здесь проверяется форма SOAP-запроса, как в REST-парсере проверяются URL-параметры.
    private fun toQuery(query: ShipQuery?): StarshipQuery {
        val q = query ?: ShipQuery()
        if (q.page < 0) throw InvalidParameterException(Messages.paramIntWithMin("page", 0))
        if (q.size < 1) throw InvalidParameterException(Messages.paramIntWithMin("size", 1))
        q.id?.let { if (it <= 0) throw InvalidParameterException(Messages.paramPositiveInt("id")) }
        q.name?.let { if (it.isBlank()) throw InvalidParameterException(Messages.paramBlank("name")) }
        val seen = mutableSetOf<StarshipField>()
        val sorts = q.sort.map { sort ->
            val soapField = sort?.field
                ?: throw InvalidParameterException(Messages.paramUnknownValue("sort", "null"))
            val field = StarshipField.valueOf(soapField.name)
            if (!seen.add(field)) throw InvalidParameterException(Messages.sortFieldDuplicated(field.apiName))
            StarshipSort(field, sort.descending)
        }
        return StarshipQuery(StarshipFilter(q.id, q.name), sorts, q.page, q.size)
    }

    private fun <T> call(operation: () -> T): T = try {
        operation()
    } catch (e: RuntimeException) {
        val fault = FaultDetail().apply {
            code = e.javaClass.simpleName
            message = e.message
            status = when (e) {
                is InvalidParameterException -> 400
                is StarshipNotFoundException, is SpaceMarineNotFoundException, is SpaceMarineNotOnBoardException -> 404
                is StarshipAlreadyExistsException, is SpaceMarineAlreadyOnBoardException -> 409
                is StarshipValidationException -> {
                    details = e.details
                    422
                }
                is SpaceMarineServiceUnavailableException -> 503
                else -> {
                    LOG.log(Level.SEVERE, Messages.UNHANDLED_ERROR, e)
                    code = "InternalError"
                    message = Messages.INTERNAL_ERROR
                    500
                }
            }
        }
        throw StarshipFault(fault.message, fault)
    }

    private companion object {
        val LOG: Logger = Logger.getLogger(StarshipEndpoint::class.java.name)
    }
}
