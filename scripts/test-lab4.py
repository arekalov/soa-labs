#!/usr/bin/env python3
r"""External REST contract smoke test; Python 3 standard library only.

Run only after coordinating access to the stand. No SSH or service control is
performed here. Example tunnels (replace SSH_HOST with your SSH alias):
  ssh -N -L 27343:127.0.0.1:27343 -L 27443:127.0.0.1:27443 \
      -L 27780:127.0.0.1:27780 SSH_HOST
  python3 scripts/test-lab4.py --ca-cert /path/to/trusted-ca.pem
  python3 scripts/test-lab4.py --starship-ca-cert certs/starship.crt \
      --spacemarine-ca-cert certs/spacemarine.crt

Stop Mule yourself before the unavailable run. That mode skips direct Mule GET
and normal boarding tests. --marine-id may reuse an existing, unboarded marine
ONLY in this mode; it is never deleted. Otherwise a new marine is always made.
URLs include the resource prefix for Starship, but not for SpaceMarine/Mule.
Environment: LAB4_STARSHIP_URL, LAB4_SPACEMARINE_URL, LAB4_MULE_URL,
LAB4_CA_CERT, LAB4_STARSHIP_CA_CERT, LAB4_SPACEMARINE_CA_CERT, LAB4_TIMEOUT.
CLI arguments override environment values. Service CA options override the common
--ca-cert fallback; trust stores are isolated by origin (scheme, host, port).
HTTPS Mule uses the common CA unless it shares a service origin. HTTP Mule needs
no CA. Without a CA option, HTTPS uses the system trust store.

No SOAP assumptions. No retries of mutations. Fixtures are recorded immediately
when a creation response identifies them; finally deletes only those objects.
A connection loss after POST can leave an unidentifiable fixture: the printed
run token/request identifies it for manual investigation (never bulk deletion).
Concurrent modifications of test fixtures are outside this smoke test's scope.
"""

import argparse
import json
import math
import os
import ssl
import sys
import uuid
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import HTTPSHandler, HTTPRedirectHandler, ProxyHandler, Request, build_opener


class Failure(Exception):
    pass


def check(condition, message):
    if not condition:
        raise Failure(message)


def integer(value, minimum=0):
    return type(value) is int and value >= minimum


def number(value):
    return type(value) in (int, float) and math.isfinite(value)


def ship_shape(value):
    check(isinstance(value, dict), 'Starship must be an object')
    check(integer(value.get('id'), 1), 'Starship.id must be positive integer')
    check(isinstance(value.get('name'), str) and value['name'], 'Starship.name missing/empty')
    crew = value.get('marines')
    check(isinstance(crew, list) and all(integer(x, 1) for x in crew), 'Invalid Starship.marines')
    check(crew == sorted(set(crew)), 'Starship.marines must be sorted and unique')
    return value


def marine_shape(value):
    check(isinstance(value, dict), 'SpaceMarine must be an object')
    check(integer(value.get('id'), 1), 'SpaceMarine.id must be positive integer')
    check(isinstance(value.get('name'), str) and value['name'], 'SpaceMarine.name missing/empty')
    coords = value.get('coordinates')
    check(isinstance(coords, dict) and type(coords.get('x')) is int
          and number(coords.get('y')) and coords['y'] <= 12, 'Invalid coordinates')
    check(number(value.get('health')) and value['health'] > 0, 'Invalid health')
    check(type(value.get('loyal')) is bool, 'Invalid loyal')
    check(value.get('category') in ('SCOUT', 'SUPPRESSOR', 'TACTICAL', 'HELIX'), 'Invalid category')
    date = value.get('creationDate')
    check(isinstance(date, str) and 'T' in date, 'Missing creationDate')
    try:
        datetime.fromisoformat(date.replace('Z', '+00:00'))
    except ValueError as exc:
        raise Failure('Invalid creationDate') from exc
    check(value.get('achievements') is None or isinstance(value['achievements'], str), 'Invalid achievements')
    chapter = value.get('chapter')
    if chapter is not None:
        check(isinstance(chapter, dict) and isinstance(chapter.get('name'), str)
              and chapter['name'], 'Invalid chapter')
        check(chapter.get('parentLegion') is None or isinstance(chapter['parentLegion'], str),
              'Invalid parentLegion')
    return value


class NoRedirect(HTTPRedirectHandler):
    # Do not silently change POST to GET or hide a contract-breaking redirect.
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def origin(url):
    parsed = urlsplit(url)
    return (parsed.scheme, parsed.hostname, parsed.port or (443 if parsed.scheme == 'https' else 80))


class Smoke:
    def __init__(self, args):
        self.args = args
        self.token = 'lab4-' + uuid.uuid4().hex
        self.special = self.token + ' Корабль <&> "Космос" \' Ω'
        self.owned = []  # (resource collection URL, id), creation order
        self.requests = 0
        self.ships = args.starship_url.rstrip('/')
        self.marines = args.spacemarine_url.rstrip('/') + '/space-marines'
        self.mule = args.mule_url.rstrip('/') + '/space-marines'
        certificates = {}
        for url, certificate in ((self.ships, args.starship_ca_cert),
                                 (self.marines, args.spacemarine_ca_cert)):
            if certificate:
                key = origin(url)
                check(key not in certificates or certificates[key] == certificate,
                      'Different service CA files cannot be assigned to the same origin')
                certificates[key] = certificate
        self.http_by_origin = {}
        for url in (self.ships, self.marines, self.mule):
            key = origin(url)
            if key in self.http_by_origin:
                continue
            if args.insecure:
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
                context.check_hostname = False
                context.verify_mode = ssl.CERT_NONE
            elif key[0] == 'https':
                context = ssl.create_default_context(cafile=certificates.get(key, args.ca_cert))
            else:
                # HTTP Mule does not need to load the system CA/keychain. Keep
                # the unused HTTPS handler strict; redirects are disabled.
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
            # Do not route local SSH tunnels through environment proxies.
            self.http_by_origin[key] = build_opener(
                ProxyHandler({}), HTTPSHandler(context=context), NoRedirect())

    def request(self, method, url, expected=200, body=None, created_at=None):
        self.requests += 1
        print('{} {} -> expected {}'.format(method, url, expected), flush=True)
        headers = {'Accept': 'application/json'}
        data = None
        if body is not None:
            headers['Content-Type'] = 'application/json; charset=utf-8'
            data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        req = Request(url, data=data, headers=headers, method=method)
        try:
            response = self.http_by_origin[origin(url)].open(req, timeout=self.args.timeout)
        except HTTPError as exc:
            response = exc
        with response:
            status = response.code
            raw = response.read()
            content_type = response.headers.get_content_type()
        value = None
        if raw:
            try:
                value = json.loads(raw.decode('utf-8'))
            except (ValueError, UnicodeError):
                pass
        # Register before checking the remaining contract, so a malformed DTO or
        # wrong Content-Type after successful creation still gets cleaned up.
        if created_at and 200 <= status < 300 and isinstance(value, dict):
            object_id = value.get('id')
            if type(object_id) is int:
                entry = (created_at, object_id)
                if entry not in self.owned:
                    self.owned.append(entry)
                    print('OWNED {} / {}'.format(*entry), flush=True)
        check(status == expected, 'HTTP {} expected {}, body={!r}'.format(status, expected, raw[:800]))
        if status == 204:
            check(not raw, '204 response must have no body')
            return None
        check(content_type == 'application/json', 'Expected application/json, got ' + content_type)
        check(isinstance(value, dict), 'Expected a JSON object, body={!r}'.format(raw[:800]))
        if status >= 400:
            check(type(value.get('code')) is int and value['code'] == status, 'Error.code mismatch')
            check(isinstance(value.get('message'), str) and value['message'], 'Missing Error.message')
            details = value.get('details')
            check(details is None or (isinstance(details, list) and all(isinstance(x, str) for x in details)),
                  'Invalid Error.details')
        return value

    def get_ship(self, object_id):
        return ship_shape(self.request('GET', self.ships + '/' + str(object_id)))

    def expect_ship(self, value, object_id, name, crew):
        ship_shape(value)
        check((value['id'], value['name'], value['marines']) == (object_id, name, sorted(crew)),
              'Unexpected Starship identity/name/crew: {!r}'.format(value))

    def create_ship(self, name):
        value = self.request('POST', self.ships, 201, {'name': name}, self.ships)
        ship_shape(value)
        self.expect_ship(value, value['id'], name, [])
        return value['id']

    def page(self, base, params, shape, page=0, size=20):
        query = list(params) + [('page', page), ('size', size)]
        value = self.request('GET', base + '?' + urlencode(query))
        for key in ('page', 'size', 'totalElements', 'totalPages'):
            check(integer(value.get(key)), 'Invalid page.' + key)
        check(value['page'] == page and value['size'] == size, 'Wrong page/size echo')
        check(value['totalPages'] == (value['totalElements'] + size - 1) // size, 'Wrong totalPages')
        items = value.get('items')
        check(isinstance(items, list), 'Missing page.items')
        check(len(items) == min(size, max(0, value['totalElements'] - page * size)), 'Wrong page length')
        for item in items:
            shape(item)
        check(len({x['id'] for x in items}) == len(items), 'Duplicate page IDs')
        return value

    def delete_owned(self, base, object_id):
        check((base, object_id) in self.owned, 'Refusing to delete an unowned object')
        self.request('DELETE', base + '/' + str(object_id), 204)
        self.owned.remove((base, object_id))
        self.request('GET', base + '/' + str(object_id), 404)

    def cleanup(self):
        errors = []
        # Ships before marines; no unload dependency on Mule during cleanup.
        entries = sorted(self.owned, key=lambda entry: entry[0] != self.ships)
        for base, object_id in entries:
            try:
                self.delete_owned(base, object_id)
            except Exception as exc:
                errors.append('{} / {}: {}'.format(base, object_id, exc))
        return errors

    def run(self):
        print('RUN ' + self.token, flush=True)
        a = self.create_ship(self.special)
        b = self.create_ship(self.special)
        self.expect_ship(self.get_ship(a), a, self.special, [])
        # Exact unique name isolates pagination/sorting from pre-existing data.
        for sorts, ordered in [(['id'], sorted([a, b])), (['name', '-id'], sorted([a, b], reverse=True))]:
            params = [('name', self.special)] + [('sort', field) for field in sorts]
            for page in range(3):
                result = self.page(self.ships, params, ship_shape, page, 1)
                check(result['totalElements'] == 2, 'Exact name filter failed')
                check([x['id'] for x in result['items']] == ordered[page:page + 1], 'Sort/page failed')
        result = self.page(self.ships, [('id', a), ('name', self.special)], ship_shape)
        check([x['id'] for x in result['items']] == [a], 'Combined filter failed')
        result = self.page(self.ships, [('id', a), ('name', self.token + '-absent')], ship_shape)
        check(result['totalElements'] == 0, 'Filters must be combined with AND')
        for params in [[('page', -1)], [('size', 0)], [('sort', 'unknown')], [('id', 0)]]:
            self.request('GET', self.ships + '?' + urlencode(params), 400)
        for name in ('', '   ', None):
            self.request('POST', self.ships, 422, {'name': name}, self.ships)
            self.request('PATCH', self.ships + '/' + str(a), 422, {'name': name})
        self.expect_ship(self.get_ship(a), a, self.special, [])
        for invalid_id in ('0', '-1', 'not-an-id'):
            self.request('POST', self.ships + '/create/' + invalid_id + '/' + quote(self.special, safe=''),
                         400, created_at=self.ships)
        self.request('POST', self.ships + '/create/' + str(a) + '/%20', 400, created_at=self.ships)
        self.request('POST', self.ships + '/create/' + str(a) + '/' + self.token, 409)
        self.expect_ship(self.get_ship(a), a, self.special, [])
        # Reuse our deleted generated ID: no huge random ID/sequence pollution.
        self.delete_owned(self.ships, b)
        self.request('DELETE', self.ships + '/' + str(b), 404)
        self.request('PATCH', self.ships + '/' + str(b), 404, {'name': self.token})
        value = self.request('POST', self.ships + '/create/' + str(b) + '/' + quote(self.special, safe=''),
                             201, created_at=self.ships)
        self.expect_ship(value, b, self.special, [])
        renamed = self.special + ' renamed'
        value = self.request('PATCH', self.ships + '/' + str(b), 200, {'name': renamed})
        self.expect_ship(value, b, renamed, [])
        self.expect_ship(self.get_ship(b), b, renamed, [])

        if self.args.marine_id is not None:
            marine = marine_shape(self.request('GET', self.marines + '/' + str(self.args.marine_id)))
            check(marine['id'] == self.args.marine_id, 'Existing marine ID mismatch')
        else:
            payload = {'name': self.special + ' marine', 'coordinates': {'x': 10, 'y': 3.5},
                       'health': 85.5, 'loyal': True, 'category': 'TACTICAL',
                       'achievements': '<&> Ω', 'chapter': {'name': self.token, 'parentLegion': None}}
            marine = self.request('POST', self.marines, 201, payload, self.marines)
            marine_shape(marine)
            for key, value in payload.items():
                check(marine.get(key) == value, 'SpaceMarine create round-trip failed: ' + key)
        mid = marine['id']
        fetched = marine_shape(self.request('GET', self.marines + '/' + str(mid)))
        check(fetched == marine, 'SpaceMarine GET differs from fixture')
        result = self.page(self.marines, [('id', mid), ('name', marine['name']),
                                        ('sort', 'name'), ('sort', '-id')], marine_shape, size=1)
        check(result['items'] == [marine], 'SpaceMarine filtered list mismatch')

        board = self.ships + '/' + str(a) + '/board/' + str(mid)
        if self.args.mule_unavailable:
            before = self.get_ship(a)
            try:
                self.request('POST', board, 503)
            finally:
                check(self.get_ship(a) == before, 'Board during Mule outage mutated the ship')
            return

        direct = marine_shape(self.request('GET', self.mule + '/' + str(mid)))
        check(direct == marine, 'Direct Mule GET differs from REST SpaceMarine fixture')
        for operation in ('board', 'unload'):
            for sid, marine_id in ((0, mid), (a, 0), ('bad', mid), (a, 'bad')):
                self.request('POST', '{}/{}/{}/{}'.format(self.ships, sid, operation, marine_id), 400)
        self.expect_ship(self.get_ship(a), a, self.special, [])
        self.request('POST', self.ships + '/' + str(a) + '/unload/' + str(mid), 404)
        value = self.request('POST', board)
        self.expect_ship(value, a, self.special, [mid])
        self.expect_ship(self.get_ship(a), a, self.special, [mid])
        self.request('POST', board, 409)
        self.request('POST', self.ships + '/' + str(b) + '/board/' + str(mid), 409)
        self.request('POST', self.ships + '/' + str(b) + '/unload/' + str(mid), 404)
        self.expect_ship(self.get_ship(a), a, self.special, [mid])
        self.expect_ship(self.get_ship(b), b, renamed, [])
        unloaded = self.request('POST', self.ships + '/' + str(a) + '/unload/' + str(mid))
        check(type(unloaded.get('starshipId')) is int and unloaded['starshipId'] == a
              and type(unloaded.get('spaceMarineId')) is int and unloaded['spaceMarineId'] == mid
              and isinstance(unloaded.get('message'), str) and unloaded['message'], 'Invalid UnloadResult')
        self.expect_ship(self.get_ship(a), a, self.special, [])
        self.request('POST', self.ships + '/' + str(a) + '/unload/' + str(mid), 404)
        # Reboarding after unload demonstrates release of the global crew lock.
        value = self.request('POST', self.ships + '/' + str(b) + '/board/' + str(mid))
        self.expect_ship(value, b, renamed, [mid])
        self.delete_owned(self.ships, b)
        for operation in ('board', 'unload'):
            self.request('POST', '{}/{}/{}/{}'.format(self.ships, b, operation, mid), 404)
        self.delete_owned(self.marines, mid)
        for operation in ('board', 'unload'):
            self.request('POST', '{}/{}/{}/{}'.format(self.ships, a, operation, mid), 404)
        self.expect_ship(self.get_ship(a), a, self.special, [])
        self.delete_owned(self.ships, a)


def arguments():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--starship-url', default=os.getenv('LAB4_STARSHIP_URL', 'https://localhost:27343/starship'))
    parser.add_argument('--spacemarine-url', default=os.getenv('LAB4_SPACEMARINE_URL', 'https://localhost:27443'))
    parser.add_argument('--mule-url', default=os.getenv('LAB4_MULE_URL', 'http://localhost:27780'))
    tls = parser.add_mutually_exclusive_group()
    tls.add_argument('--ca-cert', default=os.getenv('LAB4_CA_CERT'), help='Fallback PEM CA bundle for HTTPS origins')
    tls.add_argument('--insecure', action='store_true', help='Explicitly disable TLS certificate verification')
    parser.add_argument('--starship-ca-cert', default=os.getenv('LAB4_STARSHIP_CA_CERT'),
                        help='Starship origin CA PEM; overrides --ca-cert')
    parser.add_argument('--spacemarine-ca-cert', default=os.getenv('LAB4_SPACEMARINE_CA_CERT'),
                        help='SpaceMarine origin CA PEM; overrides --ca-cert')
    parser.add_argument('--timeout', type=float, default=os.getenv('LAB4_TIMEOUT', '20'), help='Seconds per HTTP request')
    parser.add_argument('--mule-unavailable', action='store_true', help='Expect board=503; Mule must already be stopped')
    parser.add_argument('--marine-id', type=int, help='Existing unboarded marine; outage mode only, never deleted')
    args = parser.parse_args()
    if args.insecure and (args.starship_ca_cert or args.spacemarine_ca_cert):
        parser.error('--insecure cannot be combined with service CA options')
    if not math.isfinite(args.timeout) or args.timeout <= 0:
        parser.error('--timeout must be finite and positive')
    if args.marine_id is not None and (not args.mule_unavailable or not 0 < args.marine_id <= 2147483647):
        parser.error('--marine-id requires --mule-unavailable and a positive int32')
    for url in (args.starship_url, args.spacemarine_url, args.mule_url):
        parsed = urlsplit(url)
        if parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.query or parsed.fragment or parsed.username:
            parser.error('URLs must be absolute HTTP(S) base URLs without query, fragment or credentials')
    if args.insecure:
        args.ca_cert = None
    return args


def main():
    args = arguments()
    smoke = None
    failures = []
    try:
        smoke = Smoke(args)
        smoke.run()
    except KeyboardInterrupt:
        failures.append('Interrupted')
    except Exception as exc:
        failures.append('{}: {}'.format(type(exc).__name__, exc))
    finally:
        if smoke is not None:
            failures.extend('Cleanup: ' + error for error in smoke.cleanup())
    for error in failures:
        print('FAIL: ' + error, file=sys.stderr)
    print('{} ({} HTTP requests)'.format('FAIL' if failures else 'PASS', smoke.requests if smoke else 0))
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
