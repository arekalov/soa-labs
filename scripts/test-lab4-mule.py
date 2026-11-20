#!/usr/bin/env python3
"""Exercise Mule's JSON↔SOAP adapter directly, without the Spring facade.
Requires the lab4 tunnel (27781). Creates and deletes only its own ship.
"""
import argparse
import json
import ssl
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='https://localhost:27781/starship/operations')
    parser.add_argument('--ca-cert', default='certs/starship.crt')
    args = parser.parse_args()
    context = ssl.create_default_context(cafile=args.ca_cert)
    base = args.url.rstrip('/') + '/'
    owned = []
    count = 0

    def call(operation, parameters, expected):
        nonlocal count
        count += 1
        request = Request(base + operation, json.dumps(parameters, ensure_ascii=False).encode(),
                          {'Content-Type': 'application/json', 'Accept': 'application/json'}, method='POST')
        try:
            response = urlopen(request, context=context, timeout=30)
        except HTTPError as error:
            response = error
        with response:
            status = response.status
            raw = response.read()
            value = json.loads(raw) if raw else None
            # Track before assertions so a contract failure still cleans up creations.
            if operation == 'create' and status == 201 and isinstance(value, dict) and type(value.get('id')) is int:
                owned.append(value['id'])
            assert status == expected, (operation, status, expected, value)
            if status != 204:
                assert response.headers.get_content_type() == 'application/json'
            if status >= 400:
                assert value['code'] == expected and isinstance(value['message'], str), value
            print(operation, status, flush=True)
            return value

    try:
        name = 'mule-json-' + uuid.uuid4().hex + ' Корабль <&> "Ω" 🚀'
        ship = call('create', {'name': name}, 201)
        sid = ship['id']
        assert ship == {'id': sid, 'name': name, 'marines': []}
        assert call('getById', {'id': sid}, 200) == ship
        query = {'filter': {'name': name}, 'sort': [{'field': 'NAME', 'descending': False},
                 {'field': 'ID', 'descending': True}], 'page': 0, 'size': 2}
        page = call('list', query, 200)
        assert page == {'items': [ship], 'page': 0, 'size': 2, 'totalElements': 1, 'totalPages': 1}, page
        query['page'] = 1
        assert call('list', query, 200)['items'] == []
        assert call('rename', {'id': sid, 'name': name + ' renamed'}, 200)['name'] == name + ' renamed'
        call('createWithId', {'id': sid, 'name': name}, 409)
        call('board', {'starshipId': sid, 'spaceMarineId': 2147483647}, 404)
        call('unload', {'starshipId': sid, 'spaceMarineId': 2147483647}, 404)
        call('getById', {'id': -1}, 400)
        fault = call('create', {'name': ' '}, 422)
        assert isinstance(fault['details'], list) and fault['details'], fault
        call('unknown', {}, 404)
        call('delete', {'id': sid}, 204)
        owned.remove(sid)
        call('getById', {'id': sid}, 404)
    finally:
        for sid in owned:
            call('delete', {'id': sid}, 204)
    print('PASS (%s HTTP requests)' % count)


if __name__ == '__main__':
    main()
