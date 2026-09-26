# API Changelog 1.1.0 vs. v0


## API Changes

### GET /starship
- :warning: api path removed without deprecation


### POST /starship
- :warning: api path removed without deprecation


### GET /starship/
-  endpoint added


### POST /starship/
-  endpoint added


### POST /starship/create/{id}
-  endpoint added


### POST /starship/create/{id}/
-  endpoint added


### POST /starship/create/{id}/{name}
- :warning: removed the success response with the status `201`
-  api operation id `createStarship` removed and replaced with `createWithId`
-  api tag `starship-controller` added
-  api tag `Starship` removed
-  for the `path` request parameter `id`, the exclusiveMinimum was unset from `true`
-  for the `path` request parameter `name`, the minLength was unset from `1`
-  for the `path` request parameter `id`, the min was unset from `0.00`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `409`
-  removed the non-success response with the status `500`
-  added the success response with the status `200`


### DELETE /starship/{id}
- :warning: added the new path request parameter `id`
- :warning: removed the success response with the status `204`
-  api operation id `deleteStarship` removed and replaced with `delete`
-  api tag `starship-controller` added
-  api tag `Starship CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `500`
-  added the success response with the status `200`


### GET /starship/{id}
- :warning: added the new path request parameter `id`
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
-  api operation id `getStarship` removed and replaced with `getById`
-  api tag `starship-controller` added
-  api tag `Starship CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `500`


### PATCH /starship/{id}
- :warning: added the new path request parameter `id`
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
-  api operation id `renameStarship` removed and replaced with `rename`
-  api tag `starship-controller` added
-  api tag `Starship CRUD` removed
-  request body became optional
-  the request property `name` became nullable
-  the request property `name` became optional
-  the `name` request property's minLength was unset from `1`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `422`
-  removed the non-success response with the status `500`


### POST /starship/{starship-id}/board/{space-marine-id}
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
-  api operation id `boardSpaceMarine` removed and replaced with `board`
-  api tag `starship-controller` added
-  api tag `Starship CRUD` removed
-  for the `path` request parameter `space-marine-id`, the exclusiveMinimum was unset from `true`
-  for the `path` request parameter `starship-id`, the exclusiveMinimum was unset from `true`
-  for the `path` request parameter `space-marine-id`, the min was unset from `0.00`
-  for the `path` request parameter `starship-id`, the min was unset from `0.00`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `409`
-  removed the non-success response with the status `500`
-  removed the non-success response with the status `503`


### POST /starship/{starship-id}/unload/{space-marine-id}
-  api operation id `unloadSpaceMarine` removed and replaced with `unload`
-  api tag `starship-controller` added
-  api tag `Starship` removed
-  for the `path` request parameter `space-marine-id`, the exclusiveMinimum was unset from `true`
-  for the `path` request parameter `starship-id`, the exclusiveMinimum was unset from `true`
-  for the `path` request parameter `space-marine-id`, the min was unset from `0.00`
-  for the `path` request parameter `starship-id`, the min was unset from `0.00`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `500`
-  removed the non-success response with the status `503`




## Components
-  removed the schema `Error`
-  removed the schema `Starship`
-  removed the schema `StarshipInput`
-  removed the schema `StarshipPage`
-  removed the schema `UnloadResult`




