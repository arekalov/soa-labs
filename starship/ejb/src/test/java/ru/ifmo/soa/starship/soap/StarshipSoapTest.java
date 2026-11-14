package ru.ifmo.soa.starship.soap;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import org.junit.jupiter.api.*;
import ru.ifmo.soa.starship.api.StarshipRemote;
import ru.ifmo.soa.starship.dto.*;
import ru.ifmo.soa.starship.exception.*;
import ru.ifmo.soa.starship.query.StarshipQuery;
import javax.xml.namespace.QName;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real SOAP HTTP round trips against the endpoint adapter; no database or EJB container. */
class StarshipSoapTest {
    private Endpoint endpoint;
    private StarshipPort port;
    private String address;
    private RuntimeException failure;
    private String operation;
    private Object[] arguments;

    @BeforeEach void start() throws Exception {
        int freePort;
        try (var socket = new ServerSocket(0)) { freePort = socket.getLocalPort(); }
        address = "http://127.0.0.1:" + freePort + "/StarshipService";
        var adapter = new StarshipEndpoint();
        var business = (StarshipRemote) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{StarshipRemote.class}, (proxy, method, args) -> {
                if (failure != null) throw failure;
                operation = method.getName();
                arguments = args;
                var ship = new StarshipDto(7, "Корабль & <звезда>", List.of(3, 5));
                if (operation.equals("list")) return new StarshipPageDto(List.of(ship), 2, 4, 9, 3);
                if (operation.equals("delete")) return null;
                return ship;
            });
        var field = StarshipEndpoint.class.getDeclaredField("business");
        field.setAccessible(true);
        field.set(adapter, business);
        endpoint = Endpoint.publish(address, adapter);
        port = Service.create(URI.create(address + "?wsdl").toURL(),
            new QName(StarshipPort.NS, "StarshipService"))
            .getPort(new QName(StarshipPort.NS, "StarshipPort"), StarshipPort.class);
    }

    @AfterEach void stop() {
        if (port != null) org.apache.cxf.frontend.ClientProxy.getClient(port).destroy();
        if (endpoint != null) endpoint.stop();
    }

    @Test void allEightOperationsRoundTripWithTypedWsdl() throws Exception {
        var query = new ShipQuery();
        query.id = 7L;
        query.name = "Корабль & <звезда>";
        query.page = 2;
        query.size = 4;
        var sort = new ShipSort(); sort.field = SortField.NAME; sort.descending = true;
        query.sort.add(sort);
        var page = port.list(query);
        assertEquals(9, page.totalElements);
        assertEquals(3, page.totalPages);
        assertEquals(List.of(3, 5), page.items.getFirst().marines);
        var parsed = (StarshipQuery) arguments[0];
        assertEquals(query.name, parsed.getFilter().getName());
        assertTrue(parsed.getSort().getFirst().getDescending());
        assertEquals(2, parsed.getPage());
        assertEquals(4, parsed.getSize());
        assertEquals(query.name, port.getById(7).name);
        assertEquals("getById", operation);
        port.create(null); assertEquals("create", operation); assertNull(arguments[0]);
        port.createWithId(7, query.name); assertEquals("createWithId", operation); assertEquals(query.name, arguments[1]);
        port.rename(7, "new"); assertEquals("rename", operation);
        port.board(7, 3); assertEquals("board", operation); assertEquals(3, arguments[1]);
        port.unload(7, 3); assertEquals("unload", operation);
        port.delete(7); assertEquals("delete", operation);
        String wsdl;
        try (var stream = URI.create(address + "?wsdl").toURL().openStream()) {
            wsdl = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (var name : List.of("list", "getById", "create", "createWithId", "rename", "board", "unload", "delete"))
            assertTrue(wsdl.contains("name=\"" + name + "\""), name);
        assertTrue(wsdl.contains("StarshipFault"));
        assertTrue(wsdl.contains("ShipQuery"));
        assertFalse(wsdl.contains("anyType"));
    }

    @Test void faultsPreserveStatusesMessagesAndValidationDetails() {
        var cases = new LinkedHashMap<RuntimeException, Integer>();
        cases.put(new InvalidParameterException("invalid"), 400);
        cases.put(new StarshipNotFoundException(7), 404);
        cases.put(new SpaceMarineNotFoundException(3), 404);
        cases.put(new SpaceMarineNotOnBoardException(7, 3), 404);
        cases.put(new StarshipAlreadyExistsException(7), 409);
        cases.put(new SpaceMarineAlreadyOnBoardException(7, 3), 409);
        cases.put(new StarshipValidationException(List.of("name: пустое & <значение>")), 422);
        cases.put(new SpaceMarineServiceUnavailableException("upstream", null), 503);
        for (var entry : cases.entrySet()) {
            failure = entry.getKey();
            var fault = assertThrows(StarshipFault.class, () -> port.getById(7)).getFaultInfo();
            assertEquals(entry.getValue(), fault.status);
            assertEquals(failure.getMessage(), fault.message);
            assertEquals(failure.getClass().getSimpleName(), fault.code);
            if (failure instanceof StarshipValidationException validation)
                assertEquals(validation.getDetails(), fault.details);
        }
        failure = new IllegalStateException("private internal detail");
        var fault = assertThrows(StarshipFault.class, () -> port.getById(7)).getFaultInfo();
        assertEquals(500, fault.status);
        assertFalse(fault.message.contains("private"));
    }

    @Test void malformedQueryIsTypedFaultBeforeBusinessInvocation() {
        var query = new ShipQuery(); query.size = 0;
        assertEquals(400, assertThrows(StarshipFault.class, () -> port.list(query)).getFaultInfo().status);
        assertNull(operation);
    }
}
