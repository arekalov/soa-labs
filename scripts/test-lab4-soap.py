#!/usr/bin/env python3
r"""Stdlib-only deployed WSDL/SOAP 1.1 integration smoke. No REST requests.

Run ONLY after coordinating remote tests with the stand owner. Example:
  ssh -N -L 27543:127.0.0.1:27543 SSH_HOST
  python3 scripts/test-lab4-soap.py --insecure
  python3 scripts/test-lab4-soap.py --endpoint https://localhost:27643/starship-soap/StarshipService --ca-cert CA.pem
  python3 scripts/test-lab4-soap.py --endpoint https://localhost:27343/starship-soap/StarshipService --insecure

Default endpoint: https://localhost:27543/starship-soap/StarshipService
Use --wsdl-only to validate the published contract without creating fixtures.
WSDL: endpoint + '?wsdl'. Environment: LAB4_SOAP_URL, LAB4_SOAP_CA_CERT,
LAB4_SOAP_TIMEOUT. CLI overrides environment. --help makes no requests.

Checks eight typed WSDL operations, then direct SOAP CRUD/list/filter/sort/page,
Unicode/XML-sensitive names, and typed faults 400/404/422/409 (HTTP 500).
Board/unload signatures are checked, but these operations are not invoked.
SOAPAction, wrapper QNames, field order and field namespaces come from WSDL/XSD,
including unqualified wrapper parameters and qualified DTO fields. Unsupported
schema constructs fail before mutations instead of guessing a wire format.
Published import hosts/ports are rebased to the supplied endpoint origin for SSH
tunnels; soap:address is validated but never used as a connection destination.

Only positively identified creation responses are tracked. finally deletes only
those fixture IDs, never existing objects or objects selected by a broad query.
No mutation retries: a connection loss after create may leave an unknown fixture;
the printed unique run token helps manual investigation. Cleanup errors fail the
run. This script cannot prove ownership if another actor replaces its fixture ID.
"""

import argparse
import io
import math
import os
import ssl
import sys
import uuid
import xml.etree.ElementTree as ET
from urllib.error import HTTPError
from urllib.parse import urljoin, urlsplit, urlunsplit
from urllib.request import HTTPSHandler, HTTPRedirectHandler, ProxyHandler, Request, build_opener

W = 'http://schemas.xmlsoap.org/wsdl/'
B = 'http://schemas.xmlsoap.org/wsdl/soap/'
S = 'http://schemas.xmlsoap.org/soap/envelope/'
X = 'http://www.w3.org/2001/XMLSchema'
NS = 'urn:soa:starship:v1'
I = 'http://www.w3.org/2001/XMLSchema-instance'
OPS = {
    'list': ({'query': 'ShipQuery'}, 'ShipPage'),
    'getById': ({'id': 'long'}, 'Ship'),
    'create': ({'name': 'string'}, 'Ship'),
    'createWithId': ({'id': 'long', 'name': 'string'}, 'Ship'),
    'rename': ({'id': 'long', 'name': 'string'}, 'Ship'),
    'delete': ({'id': 'long'}, None),
    'board': ({'starshipId': 'long', 'spaceMarineId': 'int'}, 'Ship'),
    'unload': ({'starshipId': 'long', 'spaceMarineId': 'int'}, 'Ship'),
}
TYPES = {
    'Ship': {'id': 'long', 'name': 'string', 'marines': 'int'},
    'ShipPage': {'items': 'Ship', 'page': 'int', 'size': 'int', 'totalElements': 'long', 'totalPages': 'int'},
    'ShipQuery': {'id': 'long', 'name': 'string', 'sort': 'ShipSort', 'page': 'int', 'size': 'int'},
    'ShipSort': {'field': 'SortField', 'descending': 'boolean'},
    'FaultDetail': {'status': 'int', 'code': 'string', 'message': 'string', 'details': 'string'},
}


def tag(namespace, local):
    return '{' + namespace + '}' + local if namespace else local


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Transport:
    def __init__(self, args):
        self.timeout = args.timeout
        if args.insecure:
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
            context.check_hostname = False
            context.verify_mode = ssl.CERT_NONE
        else:
            context = ssl.create_default_context(cafile=args.ca_cert)
        self.opener = build_opener(ProxyHandler({}), NoRedirect(), HTTPSHandler(context=context))
        self.count = 0

    def request(self, url, body=None, action=''):
        headers = {'Accept': 'text/xml, application/xml'}
        if body is not None:
            headers.update({'Content-Type': 'text/xml; charset=utf-8', 'SOAPAction': '"' + action + '"'})
        req = Request(url, data=body, headers=headers)
        self.count += 1
        try:
            response = self.opener.open(req, timeout=self.timeout)
        except HTTPError as exc:
            response = exc
        with response:
            raw = response.read(4 * 1024 * 1024 + 1)
            require(len(raw) <= 4 * 1024 * 1024, 'XML response exceeds 4 MiB')
            return response.code, response.headers.get_content_type(), raw


def parse_xml(raw):
    require(b'<!DOCTYPE' not in raw.upper() and b'<!ENTITY' not in raw.upper(), 'DTD/entities are unsupported')
    # Save namespace scope per element: QName attribute values are not expanded
    # by ElementTree and prefixes can be rebound in imported/inline schemas.
    scopes, stack, pending = {}, [], {}
    root = None
    for event, value in ET.iterparse(io.BytesIO(raw), events=('start-ns', 'start', 'end')):
        if event == 'start-ns':
            pending[value[0]] = value[1]
        elif event == 'start':
            scope = dict(stack[-1]) if stack else {}
            scope.update(pending)
            pending.clear()
            scopes[value] = scope
            stack.append(scope)
            if root is None:
                root = value
        else:
            stack.pop()
    return root, scopes


class Contract:
    def __init__(self, transport, endpoint):
        self.transport, self.endpoint = transport, endpoint
        self.scopes, self.elements, self.types, self.messages = {}, {}, {}, {}
        self.schema_of, self.wsdl, self.operations = {}, [], {}
        self.load(endpoint + '?wsdl', set())
        self.validate()

    def qname(self, node, value):
        if ':' in value:
            prefix, name = value.split(':', 1)
            require(prefix in self.scopes[node], 'Unknown QName prefix: ' + value)
            return tag(self.scopes[node][prefix], name)
        return tag(self.scopes[node].get('', ''), value)

    def load(self, url, visited):
        if url in visited:
            return
        require(len(visited) < 24, 'Too many WSDL/XSD imports')
        visited.add(url)
        print('GET ' + url, flush=True)
        status, mime, raw = self.transport.request(url)
        require(status == 200, 'WSDL/XSD HTTP {}: {!r}'.format(status, raw[:300]))
        require(mime in ('text/xml', 'application/xml'), 'WSDL/XSD is not XML: ' + mime)
        root, scopes = parse_xml(raw)
        self.scopes.update(scopes)
        require(root.tag in (tag(W, 'definitions'), tag(X, 'schema')), 'Expected WSDL definitions or XSD schema')
        if root.tag == tag(W, 'definitions'):
            self.wsdl.append(root)
            for node in root.findall(tag(W, 'message')):
                self.messages[tag(root.get('targetNamespace', ''), node.attrib['name'])] = node
        for schema in root.iter(tag(X, 'schema')):
            namespace = schema.get('targetNamespace', '')
            require(namespace, 'Chameleon schemas are unsupported')
            for node in schema.iter():
                self.schema_of[node] = schema
            for node in schema:
                if node.tag == tag(X, 'element'):
                    self.elements[tag(namespace, node.attrib['name'])] = node
                elif node.tag in (tag(X, 'complexType'), tag(X, 'simpleType')):
                    self.types[tag(namespace, node.attrib['name'])] = node
        for node in root.iter():
            location = None
            if node.tag == tag(W, 'import'):
                location = node.get('location')
            elif node.tag in (tag(X, 'import'), tag(X, 'include')):
                location = node.get('schemaLocation')
            if location:
                target = urlsplit(urljoin(url, location))
                origin = urlsplit(self.endpoint)
                require(target.scheme in ('http', 'https'), 'Non-HTTP schema import')
                imported = urlunsplit((origin.scheme, origin.netloc, target.path, target.query, ''))
                self.load(imported, visited)

    def fields(self, declaration):
        if declaration.get('type'):
            declaration = self.types[self.qname(declaration, declaration.attrib['type'])]
        elif declaration.tag == tag(X, 'element'):
            declaration = declaration.find(tag(X, 'complexType'))
        require(declaration is not None, 'Missing complex type')
        if len(declaration) == 0:
            return []
        sequence = declaration.find(tag(X, 'sequence'))
        require(sequence is not None and len(declaration) == 1, 'Only flat XSD sequences are supported')
        require(all(x.tag == tag(X, 'element') for x in sequence), 'Unsupported XSD sequence member')
        return list(sequence)

    def field_tag(self, field):
        if field.get('ref'):
            return self.qname(field, field.attrib['ref'])
        schema = self.schema_of[field]
        qualified = field.get('form', schema.get('elementFormDefault', 'unqualified')) == 'qualified'
        return tag(schema.get('targetNamespace', '') if qualified else '', field.attrib['name'])

    def signature(self, declaration):
        return {f.attrib['name']: self.qname(f, f.attrib['type']) for f in self.fields(declaration)}

    def expected_signature(self, mapping):
        return {key: (self.enum_type if value == 'SortField' else
                      tag(X if value in ('int', 'long', 'string', 'boolean') else NS, value))
                for key, value in mapping.items()}

    def message_element(self, reference):
        message = self.messages[reference]
        parts = message.findall(tag(W, 'part'))
        require(len(parts) == 1 and 'element' in parts[0].attrib, 'Expected a single typed document/literal part')
        name = self.qname(parts[0], parts[0].attrib['element'])
        require(name in self.elements, 'Missing global element ' + name)
        return name

    def validate(self):
        ports = [(root, p) for root in self.wsdl for p in root.findall(tag(W, 'portType'))
                 if tag(root.get('targetNamespace', ''), p.get('name')) == tag(NS, 'StarshipPort')]
        require(len(ports) == 1, 'Expected exactly one typed StarshipPort')
        port = ports[0][1]
        ops = port.findall(tag(W, 'operation'))
        require(len(ops) == 8 and {x.get('name') for x in ops} == set(OPS), 'WSDL must expose exactly the eight contract operations')
        bindings = [(root, b) for root in self.wsdl for b in root.findall(tag(W, 'binding'))
                    if self.qname(b, b.attrib['type']) == tag(NS, 'StarshipPort') and b.find(tag(B, 'binding')) is not None]
        require(len(bindings) == 1, 'Expected one SOAP 1.1 binding')
        root, binding = bindings[0]
        require(binding.find(tag(B, 'binding')).get('style', 'document') == 'document', 'Expected document binding')
        binding_name = tag(root.get('targetNamespace', ''), binding.attrib['name'])
        addresses = [p.find(tag(B, 'address')) for r in self.wsdl for service in r.findall(tag(W, 'service'))
                     for p in service.findall(tag(W, 'port')) if self.qname(p, p.attrib['binding']) == binding_name]
        require(any(a is not None and a.get('location') for a in addresses), 'Missing SOAP service address')
        bound = {op.attrib['name']: op for op in binding.findall(tag(W, 'operation'))}
        require(set(bound) == set(OPS), 'Binding operation mismatch')
        for op in ops:
            name = op.attrib['name']
            wrappers = []
            for direction in ('input', 'output'):
                node = op.find(tag(W, direction))
                require(node is not None, 'Missing ' + direction)
                wrappers.append(self.message_element(self.qname(node, node.attrib['message'])))
                body = bound[name].find(tag(W, direction) + '/' + tag(B, 'body'))
                require(body is not None and body.get('use') == 'literal', 'Expected literal body')
            incoming, outgoing = wrappers
            require(incoming == tag(NS, name) and outgoing == tag(NS, name + 'Response'), 'Wrapper QName mismatch')
            params, result = OPS[name]
            require(self.signature(self.elements[incoming]) == self.expected_signature(params), 'Untyped/wrong input: ' + name)
            require(self.signature(self.elements[outgoing]) == self.expected_signature({'result': result} if result else {}),
                    'Untyped/wrong output: ' + name)
            faults = op.findall(tag(W, 'fault'))
            require(len(faults) == 1, 'Missing typed fault: ' + name)
            fault_name = self.message_element(self.qname(faults[0], faults[0].attrib['message']))
            require(fault_name == tag(NS, 'StarshipFault'), 'Wrong fault element')
            fault = self.elements[fault_name]
            require(self.qname(fault, fault.attrib['type']) == tag(NS, 'FaultDetail'), 'Untyped fault')
            fault_binding = bound[name].find(tag(W, 'fault') + '/' + tag(B, 'fault'))
            require(fault_binding is not None and fault_binding.get('use') == 'literal', 'Missing literal fault binding')
            soap_op = bound[name].find(tag(B, 'operation'))
            require(soap_op is not None, 'Missing SOAP operation')
            self.operations[name] = (incoming, outgoing, soap_op.get('soapAction', ''))
        # SortField has @XmlEnum but no explicit @XmlType name. Resolve the
        # published QName (JAXB may use 'sortField') instead of guessing it.
        sort_field = next(f for f in self.fields(self.types[tag(NS, 'ShipSort')]) if f.get('name') == 'field')
        self.enum_type = self.qname(sort_field, sort_field.attrib['type'])
        require(self.enum_type.startswith('{' + NS + '}'), 'SortField must belong to the contract namespace')
        for name, signature in TYPES.items():
            require(self.signature(self.types[tag(NS, name)]) == self.expected_signature(signature), 'Wrong DTO type: ' + name)
        for typename, fieldname in (('Ship', 'marines'), ('ShipPage', 'items'), ('ShipQuery', 'sort'), ('FaultDetail', 'details')):
            field = next(f for f in self.fields(self.types[tag(NS, typename)]) if f.get('name') == fieldname)
            require(field.get('maxOccurs') == 'unbounded', 'Expected array: ' + typename + '.' + fieldname)
        restriction = self.types[self.enum_type].find(tag(X, 'restriction'))
        require(restriction is not None and self.qname(restriction, restriction.attrib['base']) == tag(X, 'string')
                and {x.get('value') for x in restriction.findall(tag(X, 'enumeration'))} == {'ID', 'NAME'}, 'Wrong SortField enum')
        print('PASS WSDL: 8 typed operations, DTOs, faults, document/literal SOAP 1.1', flush=True)

    def encode(self, parent, declaration, values):
        fields = self.fields(declaration)
        require(set(values) <= {f.attrib['name'] for f in fields}, 'Unknown input fields')
        for field in fields:
            name = field.attrib['name']
            if name not in values:
                require(field.get('minOccurs', '1') == '0', 'Required input field: ' + name)
                continue
            values_list = values[name] if field.get('maxOccurs') == 'unbounded' else [values[name]]
            for value in values_list:
                node = ET.SubElement(parent, self.field_tag(field))
                if isinstance(value, dict):
                    self.encode(node, field, value)
                else:
                    node.text = str(value).lower() if type(value) is bool else str(value)

    def decode(self, node, declaration):
        fields = self.fields(declaration)
        require(all(child.tag in {self.field_tag(f) for f in fields} for child in node), 'Unexpected XML field/namespace in ' + node.tag)
        result = {}
        for field in fields:
            name = field.attrib['name']
            nodes = node.findall(self.field_tag(field))
            repeated = field.get('maxOccurs') == 'unbounded'
            require(repeated or len(nodes) <= 1, 'Duplicate XML field: ' + name)
            require(len(nodes) >= int(field.get('minOccurs', '1')), 'Missing XML field: ' + name)
            values = []
            kind = self.qname(field, field.attrib['type'])
            for child in nodes:
                require(child.get(tag(I, 'nil')) not in ('true', '1'), 'Unexpected nil: ' + name)
                text = child.text or ''
                if kind in (tag(X, 'int'), tag(X, 'long')):
                    require(len(child) == 0, 'Nested integer XML')
                    value = int(text)
                    bits = 32 if kind == tag(X, 'int') else 64
                    require(-(2 ** (bits - 1)) <= value < 2 ** (bits - 1), 'Integer out of range')
                elif kind == tag(X, 'boolean'):
                    require(text in ('true', 'false', '1', '0'), 'Invalid boolean')
                    value = text in ('true', '1')
                elif kind in (tag(X, 'string'), self.enum_type):
                    require(len(child) == 0, 'Nested string XML')
                    value = text
                else:
                    value = self.decode(child, field)
                values.append(value)
            if repeated:
                result[name] = values
            elif values:
                result[name] = values[0]
        return result


class Smoke:
    def __init__(self, transport, contract):
        self.transport, self.contract = transport, contract
        self.owned = set()
        self.token = 'lab4-soap-' + uuid.uuid4().hex
        self.name = self.token + ' Корабль <&> "Ω" \' 🚀'

    def call(self, operation, values, fault=None):
        incoming, outgoing, action = self.contract.operations[operation]
        envelope = ET.Element(tag(S, 'Envelope'))
        body = ET.SubElement(envelope, tag(S, 'Body'))
        wrapper = ET.SubElement(body, incoming)
        self.contract.encode(wrapper, self.contract.elements[incoming], values)
        print('SOAP {} {}{}'.format(operation, values, ' expect fault=' + str(fault) if fault else ''), flush=True)
        status, mime, raw = self.transport.request(self.contract.endpoint, ET.tostring(envelope, encoding='utf-8', xml_declaration=True), action)
        root, _ = parse_xml(raw)
        require(root.tag == tag(S, 'Envelope'), 'Expected SOAP 1.1 envelope')
        bodies = root.findall(tag(S, 'Body'))
        require(len(bodies) == 1 and len(bodies[0]) == 1, 'Expected single SOAP body payload')
        payload = bodies[0][0]
        if payload.tag == tag(S, 'Fault'):
            require(status == 500, 'SOAP Fault must have HTTP 500, got ' + str(status))
            require(mime == 'text/xml', 'SOAP Fault Content-Type must be text/xml')
            require(payload.findtext('faultcode') and payload.findtext('faultstring'), 'Missing SOAP faultcode/faultstring')
            detail = payload.find('detail')
            require(detail is not None and len(detail) == 1 and detail[0].tag == tag(NS, 'StarshipFault'), 'Missing typed StarshipFault detail')
            decoded = self.contract.decode(detail[0], self.contract.elements[tag(NS, 'StarshipFault')])
            require(fault is not None and decoded.get('status') == fault, 'Unexpected SOAP Fault: ' + repr(decoded))
            expected_code = {400: 'InvalidParameterException', 404: 'StarshipNotFoundException',
                             422: 'StarshipValidationException', 409: 'StarshipAlreadyExistsException'}[fault]
            require(decoded.get('code') == expected_code and decoded.get('message'), 'Wrong fault code/message: ' + repr(decoded))
            if fault == 422:
                require(decoded.get('details') and all(decoded['details']), '422 must include validation details')
            return decoded
        require(payload.tag == outgoing, 'Unexpected response wrapper: ' + payload.tag)
        # Track ID before full decoding/status checks: other response fields may
        # violate the contract after a successful persistent create.
        if operation in ('create', 'createWithId'):
            result_field = self.contract.fields(self.contract.elements[outgoing])[0]
            result = payload.find(self.contract.field_tag(result_field))
            id_field = next(f for f in self.contract.fields(result_field) if f.get('name') == 'id')
            id_node = result.find(self.contract.field_tag(id_field)) if result is not None else None
            if id_node is not None:
                object_id = int(id_node.text)
                if object_id > 0 and 200 <= status < 300:
                    self.owned.add(object_id)
                    print('OWNED ship ' + str(object_id), flush=True)
        require(status == 200 and mime == 'text/xml', 'Expected HTTP 200 text/xml, got {} {}'.format(status, mime))
        require(fault is None, 'Expected SOAP fault {}, received success'.format(fault))
        decoded = self.contract.decode(payload, self.contract.elements[outgoing])
        return decoded.get('result')

    def ship(self, value, object_id, name):
        require(value == {'id': object_id, 'name': name, 'marines': []}, 'Ship round-trip mismatch: ' + repr(value))

    def delete(self, object_id):
        require(object_id in self.owned, 'Refusing to delete unowned fixture')
        self.call('delete', {'id': object_id})
        self.owned.remove(object_id)
        self.call('getById', {'id': object_id}, fault=404)

    def run(self):
        print('RUN ' + self.token, flush=True)
        a = self.call('create', {'name': self.name})
        require(a and type(a.get('id')) is int and a['id'] > 0, 'Missing created ID')
        aid = a['id']
        self.ship(a, aid, self.name)
        self.ship(self.call('getById', {'id': aid}), aid, self.name)
        b = self.call('create', {'name': self.name})
        require(b and type(b.get('id')) is int and b['id'] > 0 and b['id'] != aid, 'Invalid second fixture ID')
        bid = b['id']
        self.ship(b, bid, self.name)
        for descending in (False, True):
            ids = sorted([aid, bid], reverse=descending)
            for page in range(3):
                query = {'name': self.name, 'page': page, 'size': 1,
                         'sort': [{'field': 'NAME', 'descending': False}, {'field': 'ID', 'descending': descending}]}
                result = self.call('list', {'query': query})
                expected_items = [{'id': x, 'name': self.name, 'marines': []} for x in ids[page:page+1]]
                require(result == dict(items=expected_items, page=page, size=1, totalElements=2, totalPages=2), 'List/filter/sort/page mismatch: ' + repr(result))
        for name, count in ((self.name, 1), (self.token + '-absent', 0)):
            result = self.call('list', {'query': {'id': aid, 'name': name, 'page': 0, 'size': 20}})
            require(result == dict(items=[a] if count else [], page=0, size=20, totalElements=count, totalPages=count), 'AND filter failed')
        for op, params, status in [
            ('getById', {'id': 0}, 400),
            ('createWithId', {'id': 0, 'name': self.name}, 400),
            ('createWithId', {'id': aid, 'name': '   '}, 400),
            ('createWithId', {'id': aid, 'name': self.token}, 409),
            ('create', {'name': ''}, 422),
            ('rename', {'id': aid, 'name': ''}, 422),
            ('list', {'query': {'page': -1, 'size': 1}}, 400),
        ]:
            self.call(op, params, fault=status)
        self.ship(self.call('getById', {'id': aid}), aid, self.name)
        renamed = self.name + ' renamed'
        self.ship(self.call('rename', {'id': aid, 'name': renamed}), aid, renamed)
        self.ship(self.call('getById', {'id': aid}), aid, renamed)
        self.delete(bid)
        self.call('delete', {'id': bid}, fault=404)
        self.call('rename', {'id': bid, 'name': self.name}, fault=404)
        # Our deleted generated ID avoids random high IDs/sequence pollution.
        self.ship(self.call('createWithId', {'id': bid, 'name': self.name}), bid, self.name)
        self.ship(self.call('getById', {'id': bid}), bid, self.name)
        self.delete(bid)
        self.delete(aid)

    def cleanup(self):
        errors = []
        for object_id in sorted(self.owned):
            try:
                self.delete(object_id)
            except Exception as exc:
                errors.append('cleanup ship {}: {}'.format(object_id, exc))
        return errors


def arguments():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--endpoint', default=os.getenv('LAB4_SOAP_URL', 'https://localhost:27543/starship-soap/StarshipService'))
    tls = parser.add_mutually_exclusive_group()
    tls.add_argument('--ca-cert', default=os.getenv('LAB4_SOAP_CA_CERT'), help='Trusted CA PEM bundle')
    tls.add_argument('--insecure', action='store_true', help='Explicitly disable TLS certificate verification')
    parser.add_argument('--timeout', type=float, default=os.getenv('LAB4_SOAP_TIMEOUT', '30'))
    parser.add_argument('--wsdl-only', action='store_true', help='Validate WSDL/XSD only; no SOAP mutations')
    args = parser.parse_args()
    url = urlsplit(args.endpoint)
    if url.scheme not in ('http', 'https') or not url.hostname or url.username or url.query or url.fragment:
        parser.error('--endpoint must be an absolute HTTP(S) URL without credentials, query or fragment')
    if not math.isfinite(args.timeout) or args.timeout <= 0:
        parser.error('--timeout must be finite and positive')
    return args


def main():
    args = arguments()
    smoke, transport, errors = None, None, []
    try:
        transport = Transport(args)
        contract = Contract(transport, args.endpoint)
        if not args.wsdl_only:
            smoke = Smoke(transport, contract)
            smoke.run()
    except KeyboardInterrupt:
        errors.append('Interrupted')
    except Exception as exc:
        errors.append('{}: {}'.format(type(exc).__name__, exc))
    finally:
        if smoke is not None:
            errors.extend(smoke.cleanup())
    for error in errors:
        print('FAIL: ' + error, file=sys.stderr)
    print('{} ({} HTTP requests)'.format('FAIL' if errors else 'PASS', transport.count if transport else 0))
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())
