package ru.ifmo.soa.starship.soap

import jakarta.xml.ws.Endpoint
import jakarta.xml.ws.Service
import org.apache.cxf.frontend.ClientProxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.ifmo.soa.starship.api.StarshipRemote
import ru.ifmo.soa.starship.dto.StarshipDto
import ru.ifmo.soa.starship.dto.StarshipPageDto
import ru.ifmo.soa.starship.exception.*
import ru.ifmo.soa.starship.query.StarshipQuery
import java.lang.reflect.Proxy
import java.net.ServerSocket
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import javax.xml.namespace.QName

/** Настоящие SOAP-вызовы по HTTP; бизнес-компонент заменён заглушкой без БД и EJB-контейнера. */
class StarshipSoapTest {
    private var endpoint: Endpoint? = null
    private var port: StarshipPort? = null
    private lateinit var address: String
    private var failure: RuntimeException? = null
    private var operation: String? = null
    private var arguments: Array<out Any?> = emptyArray()
    private val client: StarshipPort get() = requireNotNull(port)

    @BeforeEach
    fun start() {
        val freePort = ServerSocket(0).use { it.localPort }
        address = "http://127.0.0.1:$freePort/StarshipService"
        val adapter = StarshipEndpoint()
        val business = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(StarshipRemote::class.java)) { _, method, args ->
            failure?.let { throw it }
            operation = method.name
            arguments = args ?: emptyArray()
            val ship = StarshipDto(7, "Корабль & <звезда>", listOf(3, 5))
            when (operation) {
                "list" -> StarshipPageDto(listOf(ship), 2, 4, 9, 3)
                "delete" -> null
                else -> ship
            }
        } as StarshipRemote
        StarshipEndpoint::class.java.getDeclaredField("business").apply {
            isAccessible = true
            set(adapter, business)
        }
        endpoint = Endpoint.publish(address, adapter)
        port = Service.create(URI.create("$address?wsdl").toURL(), QName(StarshipPort.NS, "StarshipService"))
            .getPort(QName(StarshipPort.NS, "StarshipPort"), StarshipPort::class.java)
    }

    @AfterEach
    fun stop() {
        port?.let { ClientProxy.getClient(it).destroy() }
        endpoint?.stop()
    }

    @Test
    fun allEightOperationsRoundTripWithTypedWsdl() {
        val query = ShipQuery().apply {
            id = 7
            name = "Корабль & <звезда>"
            page = 2
            size = 4
            sort = listOf(ShipSort().apply { field = SortField.NAME; descending = true })
        }
        val page = client.list(query)
        assertEquals(9L, page.totalElements)
        assertEquals(3, page.totalPages)
        assertEquals(listOf(3, 5), page.items.first().marines)
        val parsed = arguments[0] as StarshipQuery
        assertEquals(query.name, parsed.filter.name)
        assertTrue(parsed.sort.first().descending)
        assertEquals(2, parsed.page)
        assertEquals(4, parsed.size)
        assertEquals(query.name, client.getById(7).name)
        assertEquals("getById", operation)
        client.create(null)
        assertEquals("create", operation)
        assertNull(arguments[0])
        client.createWithId(7, query.name)
        assertEquals("createWithId", operation)
        assertEquals(query.name, arguments[1])
        client.rename(7, "new")
        assertEquals("rename", operation)
        client.board(7, 3)
        assertEquals("board", operation)
        assertEquals(3, arguments[1])
        client.unload(7, 3)
        assertEquals("unload", operation)
        client.delete(7)
        assertEquals("delete", operation)
        val wsdl = URI.create("$address?wsdl").toURL().readText()
        for (name in listOf("list", "getById", "create", "createWithId", "rename", "board", "unload", "delete")) {
            assertTrue(wsdl.contains("name=\"$name\""), name)
        }
        assertTrue(wsdl.contains("StarshipFault"))
        assertTrue(wsdl.contains("ShipQuery"))
        assertFalse(wsdl.contains("anyType"))
    }

    @Test
    fun wsdlPreservesPublishedContract() {
        val current = URI.create("$address?wsdl").toURL().readText()
        val reference = Files.readString(Path.of(System.getProperty("starship.referenceWsdl")))
        assertEquals(contract(reference), contract(current))
    }

    // Compare expanded XML names, not generated prefixes/formatting or the test server's port.
    // Local element qualification is normalized across package-level and field-level JAXB annotations.
    private fun contract(xml: String): String {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val root = factory.newDocumentBuilder().parse(xml.byteInputStream()).documentElement
        fun canonical(element: Element, elementForm: String = "unqualified"): String {
            val schema = element.namespaceURI == "http://www.w3.org/2001/XMLSchema"
            val form = if (schema && element.localName == "schema") {
                element.getAttribute("elementFormDefault").ifEmpty { "unqualified" }
            } else elementForm
            val attributes = sortedMapOf<String, String>()
            for (index in 0 until element.attributes.length) {
                val attr = element.attributes.item(index)
                if (attr.namespaceURI == "http://www.w3.org/2000/xmlns/") continue
                if (schema && element.localName == "schema" && attr.nodeName in
                    setOf("elementFormDefault", "attributeFormDefault", "version")) continue
                var value = attr.nodeValue
                if (attr.nodeName in setOf("type", "element", "message", "binding", "base")) {
                    val prefix = value.substringBefore(':', "")
                    value = "{${element.lookupNamespaceURI(prefix.ifEmpty { null }) ?: ""}}${value.substringAfter(':')}"
                }
                if (element.localName == "address" && attr.nodeName == "location") value = "ENDPOINT"
                attributes["{${attr.namespaceURI ?: ""}}${attr.localName ?: attr.nodeName}"] = value
            }
            if (schema && element.localName == "element" &&
                (element.parentNode as? Element)?.localName != "schema" && !element.hasAttribute("ref")) {
                attributes["{}form"] = element.getAttribute("form").ifEmpty { form }
            }
            val children = (0 until element.childNodes.length).mapNotNull {
                element.childNodes.item(it) as? Element
            }.map { canonical(it, form) }
            // Declaration order is irrelevant; sequence order remains part of the contract.
            val ordered = if (element.localName in setOf("definitions", "schema", "portType", "binding")) {
                children.sorted()
            } else children
            return "{${element.namespaceURI}}${element.localName}$attributes[${ordered.joinToString(";")}]"
        }
        return canonical(root)
    }

    @Test
    fun faultsPreserveStatusesMessagesAndValidationDetails() {
        val cases = linkedMapOf(
            InvalidParameterException("invalid") to 400,
            StarshipNotFoundException(7) to 404,
            SpaceMarineNotFoundException(3) to 404,
            SpaceMarineNotOnBoardException(7, 3) to 404,
            StarshipAlreadyExistsException(7) to 409,
            SpaceMarineAlreadyOnBoardException(7, 3) to 409,
            StarshipValidationException(listOf("name: пустое & <значение>")) to 422,
            SpaceMarineServiceUnavailableException("upstream", null) to 503,
        )
        for ((exception, status) in cases) {
            failure = exception
            val fault = assertThrows(StarshipFault::class.java) { client.getById(7) }.faultInfo
            assertEquals(status, fault.status)
            assertEquals(exception.message, fault.message)
            assertEquals(exception.javaClass.simpleName, fault.code)
            if (exception is StarshipValidationException) assertEquals(exception.details, fault.details)
        }
        failure = IllegalStateException("private internal detail")
        val fault = assertThrows(StarshipFault::class.java) { client.getById(7) }.faultInfo
        assertEquals(500, fault.status)
        assertFalse(requireNotNull(fault.message).contains("private"))
    }

    @Test
    fun malformedQueryIsTypedFaultBeforeBusinessInvocation() {
        val query = ShipQuery().apply { size = 0 }
        assertEquals(400, assertThrows(StarshipFault::class.java) { client.list(query) }.faultInfo.status)
        assertNull(operation)
    }
}
