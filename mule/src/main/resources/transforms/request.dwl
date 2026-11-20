%dw 2.0
output application/xml
ns env http://schemas.xmlsoap.org/soap/envelope/
ns s urn:soa:starship:v1
var p = vars.args
var operationBody = vars.operation match {
    case "list" -> {s#list: {s#query: {
        (s#id: p.filter.id) if (p.filter.id != null),
        (s#name: p.filter.name) if (p.filter.name != null),
        ((p.sort default []) map (v) -> {s#sort: {s#field: v.field, s#descending: v.descending}}),
        s#page: p.page, s#size: p.size
    }}}
    case "getById" -> {s#getById: {s#id: p.id}}
    case "create" -> {s#create: {(s#name: p.name) if (p.name != null)}}
    case "createWithId" -> {s#createWithId: {s#id: p.id, (s#name: p.name) if (p.name != null)}}
    case "rename" -> {s#rename: {s#id: p.id, (s#name: p.name) if (p.name != null)}}
    case "delete" -> {s#delete: {s#id: p.id}}
    case "board" -> {s#board: {s#starshipId: p.starshipId, s#spaceMarineId: p.spaceMarineId}}
    case "unload" -> {s#unload: {s#starshipId: p.starshipId, s#spaceMarineId: p.spaceMarineId}}
}
---
{env#Envelope: {env#Body: operationBody}}
