package ru.ifmo.soa.starship.soap;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebService;
import ru.ifmo.soa.starship.Messages;
import ru.ifmo.soa.starship.api.StarshipRemote;
import ru.ifmo.soa.starship.dto.StarshipDto;
import ru.ifmo.soa.starship.exception.*;
import ru.ifmo.soa.starship.query.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** SOAP transport only: transactions and domain decisions belong to StarshipBean. */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@WebService(endpointInterface = "ru.ifmo.soa.starship.soap.StarshipPort",
    serviceName = "StarshipService", portName = "StarshipPort", targetNamespace = StarshipPort.NS)
public class StarshipEndpoint implements StarshipPort {
    private static final Logger LOG = Logger.getLogger(StarshipEndpoint.class.getName());
    @EJB(lookup = "java:global/starship/starship-ejb/StarshipBean!ru.ifmo.soa.starship.api.StarshipRemote")
    private StarshipRemote business;

    public ShipPage list(ShipQuery query) throws StarshipFault {
        return call(() -> {
            var result = business.list(toQuery(query));
            var page = new ShipPage();
            page.items = result.getItems().stream().map(StarshipEndpoint::ship).toList();
            page.page = result.getPage();
            page.size = result.getSize();
            page.totalElements = result.getTotalElements();
            page.totalPages = result.getTotalPages();
            return page;
        });
    }
    public Ship getById(long id) throws StarshipFault { return call(() -> ship(business.getById(id))); }
    public Ship create(String name) throws StarshipFault { return call(() -> ship(business.create(name))); }
    public Ship createWithId(long id, String name) throws StarshipFault { return call(() -> ship(business.createWithId(id, name))); }
    public Ship rename(long id, String name) throws StarshipFault { return call(() -> ship(business.rename(id, name))); }
    public void delete(long id) throws StarshipFault { call(() -> { business.delete(id); return null; }); }
    public Ship board(long starshipId, int spaceMarineId) throws StarshipFault { return call(() -> ship(business.board(starshipId, spaceMarineId))); }
    public Ship unload(long starshipId, int spaceMarineId) throws StarshipFault { return call(() -> ship(business.unload(starshipId, spaceMarineId))); }

    private static Ship ship(StarshipDto dto) {
        var ship = new Ship();
        ship.id = dto.getId();
        ship.name = dto.getName();
        ship.marines = dto.getMarines();
        return ship;
    }

    // Validate SOAP query shape just as the REST query parser validates URL parameters.
    private static StarshipQuery toQuery(ShipQuery q) {
        if (q == null) q = new ShipQuery();
        if (q.page < 0) throw new InvalidParameterException(Messages.INSTANCE.paramIntWithMin("page", 0));
        if (q.size < 1) throw new InvalidParameterException(Messages.INSTANCE.paramIntWithMin("size", 1));
        if (q.id != null && q.id <= 0) throw new InvalidParameterException(Messages.INSTANCE.paramPositiveInt("id"));
        if (q.name != null && q.name.isBlank()) throw new InvalidParameterException(Messages.INSTANCE.paramBlank("name"));
        var sorts = new ArrayList<StarshipSort>();
        var seen = EnumSet.noneOf(StarshipField.class);
        for (var sort : q.sort) {
            if (sort == null || sort.field == null) throw new InvalidParameterException(Messages.INSTANCE.paramUnknownValue("sort", "null"));
            var field = StarshipField.valueOf(sort.field.name());
            if (!seen.add(field)) throw new InvalidParameterException(Messages.INSTANCE.sortFieldDuplicated(field.getApiName()));
            sorts.add(new StarshipSort(field, sort.descending));
        }
        return new StarshipQuery(new StarshipFilter(q.id, q.name), sorts, q.page, q.size);
    }

    private static <T> T call(Supplier<T> operation) throws StarshipFault {
        try { return operation.get(); }
        catch (RuntimeException e) {
            var fault = new FaultDetail();
            fault.code = e.getClass().getSimpleName();
            fault.message = e.getMessage();
            if (e instanceof InvalidParameterException) fault.status = 400;
            else if (e instanceof StarshipNotFoundException || e instanceof SpaceMarineNotFoundException || e instanceof SpaceMarineNotOnBoardException) fault.status = 404;
            else if (e instanceof StarshipAlreadyExistsException || e instanceof SpaceMarineAlreadyOnBoardException) fault.status = 409;
            else if (e instanceof StarshipValidationException validation) {
                fault.status = 422;
                fault.details = validation.getDetails();
            } else if (e instanceof SpaceMarineServiceUnavailableException) fault.status = 503;
            else {
                LOG.log(Level.SEVERE, Messages.UNHANDLED_ERROR, e);
                fault.status = 500;
                fault.code = "InternalError";
                fault.message = Messages.INTERNAL_ERROR;
            }
            throw new StarshipFault(fault.message, fault);
        }
    }
}
