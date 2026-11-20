%dw 2.0
output application/java
ns env http://schemas.xmlsoap.org/soap/envelope/
ns s urn:soa:starship:v1
var body = payload.env#Envelope.env#Body
var fault = body.env#Fault
var detail = fault.detail.s#StarshipFault
var result = (valuesOf(body)[0]).s#result
fun ship(x) = {id: x.s#id as Number, name: x.s#name as String,
    marines: (x.*s#marines default []) filter ($ != null) map ($ as Number)}
var faultStatus = (detail.s#status default 500) as Number
---
if (fault != null)
    {status: if ([400,404,409,422,500,503] contains faultStatus) faultStatus else 500,
     body: {code: if ([400,404,409,422,500,503] contains faultStatus) faultStatus else 500,
            message: detail.s#message default "Внутренняя ошибка сервера",
            details: if (isEmpty(detail.*s#details default [])) null else detail.*s#details}}
else if (attributes.statusCode != 200 or body == null)
    {status: 503, body: {code: 503, message: "Сервис Starship недоступен, повторите запрос позже", details: null}}
else if (vars.operation == "delete") {status: 204, body: null}
else if (vars.operation == "unload")
    {status: 200, body: {starshipId: vars.args.starshipId, spaceMarineId: vars.args.spaceMarineId,
     message: "Десантник " ++ (vars.args.spaceMarineId as String) ++ " высажен с корабля " ++ (vars.args.starshipId as String)}}
else if (vars.operation == "list")
    {status: 200, body: {items: (result.*s#items default []) filter ($ != null) map ship($),
        page: result.s#page as Number, size: result.s#size as Number,
        totalElements: result.s#totalElements as Number, totalPages: result.s#totalPages as Number}}
else {status: if (["create", "createWithId"] contains vars.operation) 201 else 200, body: ship(result)}
