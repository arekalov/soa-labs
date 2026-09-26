# API Changelog 1.0.0 vs. 1.0.0


## API Changes

### GET /space-marines
- :warning: the response property `items/items/coordinates/x` became nullable for the status `200`
- :warning: the response property `items/items/coordinates/y` became nullable for the status `200`
- :warning: the response property `items/items/coordinates/x` became optional for the status `200`
- :warning: the response property `items/items/coordinates/y` became optional for the status `200`
- :warning: the `items/items/health` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `items/items/id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `items/items/coordinates/y` response property's max was unset from `12.00` for the response status `200`
- :warning: the `items/items/name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `items/items/health` response property's min was unset from `0.00` for the response status `200`
- :warning: the `items/items/id` response property's min was unset from `0.00` for the response status `200`
- :warning: deleted the `query` request parameter `achievements`
- :warning: deleted the `query` request parameter `category`
- :warning: deleted the `query` request parameter `chapterName`
- :warning: deleted the `query` request parameter `chapterParentLegion`
- :warning: deleted the `query` request parameter `coordinatesX`
- :warning: deleted the `query` request parameter `coordinatesY`
- :warning: deleted the `query` request parameter `creationDate`
- :warning: deleted the `query` request parameter `health`
- :warning: deleted the `query` request parameter `id`
- :warning: deleted the `query` request parameter `loyal`
- :warning: deleted the `query` request parameter `name`
- :warning: deleted the `query` request parameter `page`
- :warning: deleted the `query` request parameter `size`
- :warning: deleted the `query` request parameter `sort`
- :warning: removed `#/components/schemas/Chapter` from the `items/items/chapter` response property `allOf` list for the response status `200`
- :warning: the `items/items/chapter` response's property `type` changed from `object` to `any` for status `200`
-  api operation id `getSpaceMarines` removed and replaced with `list`
-  api tag `SpaceMarine CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `500`
-  added `#/components/schemas/ChapterDto` to the `items/items/chapter` response property `allOf` list for the response status `200`
-  the response property `items/items/achievements` became required for the status `200`
-  the response property `items/items/chapter` became required for the status `200`
-  the `items/items/coordinates/x` response's property `format` was narrowed from `none` to `int32` for status `200`
-  the response required property `items/items/creationDate` became not read-only for the status `200`
-  the response required property `items/items/id` became not read-only for the status `200`


### POST /space-marines
- :warning: removed the success response with the status `201`
- :warning: added `#/components/schemas/AstartesCategory` to the `category` request property `allOf` list
- :warning: added `#/components/schemas/ChapterDto` to the `chapter` request property `allOf` list
- :warning: added `#/components/schemas/CoordinatesDto` to the `coordinates` request property `allOf` list
- :warning: removed the enum value `HELIX` of the request property `category`
- :warning: removed the enum value `SCOUT` of the request property `category`
- :warning: removed the enum value `SUPPRESSOR` of the request property `category`
- :warning: removed the enum value `TACTICAL` of the request property `category`
- :warning: removed the request property `coordinates/x`
- :warning: removed the request property `coordinates/y`
-  api operation id `createSpaceMarine` removed and replaced with `create`
-  api tag `SpaceMarine CRUD` removed
-  removed `#/components/schemas/Chapter` from the `chapter` request property `allOf` list
-  the request property `category` became nullable
-  the request property `coordinates` became nullable
-  the request property `health` became nullable
-  the request property `loyal` became nullable
-  the request property `name` became nullable
-  the request property `category` became optional
-  the request property `coordinates` became optional
-  the request property `health` became optional
-  the request property `loyal` became optional
-  the request property `name` became optional
-  the `health` request property's exclusiveMinimum was unset from `true`
-  the `name` request property's minLength was unset from `1`
-  the `health` request property's min was unset from `0.00`
-  the `category` request property `type` was widened from `string` to `any`
-  the `chapter` request property `type` was widened from `object` to `any`
-  the `coordinates` request property `type` was widened from `object` to `any`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `422`
-  removed the non-success response with the status `500`
-  added the success response with the status `200`


### GET /space-marines/count/by-chapter
- :warning: the `query` request parameter `parentLegion` became required
-  api operation id `countSpaceMarinesByChapter` removed and replaced with `countByChapter`
-  api tag `SpaceMarine extra` removed
-  the `query` request parameter `name` became nullable
-  the `query` request parameter `parentLegion` became nullable
-  for the `query` request parameter `name`, the minLength was unset from `1`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `500`


### GET /space-marines/count/by-health-greater-than
-  api operation id `countSpaceMarinesByHealthGreaterThan` removed and replaced with `countByHealthGreaterThan`
-  api tag `SpaceMarine extra` removed
-  the `query` request parameter `health` became nullable
-  for the `query` request parameter `health`, the `type/format` was widened from `number/float` to `string`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `500`


### GET /space-marines/search/by-name-prefix
- :warning: the `query` request parameter `page` became required
- :warning: the `query` request parameter `size` became required
- :warning: the response property `items/items/coordinates/x` became nullable for the status `200`
- :warning: the response property `items/items/coordinates/y` became nullable for the status `200`
- :warning: the response property `items/items/coordinates/x` became optional for the status `200`
- :warning: the response property `items/items/coordinates/y` became optional for the status `200`
- :warning: the `items/items/health` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `items/items/id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `items/items/coordinates/y` response property's max was unset from `12.00` for the response status `200`
- :warning: the `items/items/name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `items/items/health` response property's min was unset from `0.00` for the response status `200`
- :warning: the `items/items/id` response property's min was unset from `0.00` for the response status `200`
- :warning: removed `#/components/schemas/Chapter` from the `items/items/chapter` response property `allOf` list for the response status `200`
- :warning: the `items/items/chapter` response's property `type` changed from `object` to `any` for status `200`
-  api operation id `findSpaceMarinesByNamePrefix` removed and replaced with `findByNamePrefix`
-  api tag `SpaceMarine extra` removed
-  the `query` request parameter `page` became nullable
-  the `query` request parameter `prefix` became nullable
-  the `query` request parameter `size` became nullable
-  for the `query` request parameter `prefix`, the minLength was unset from `1`
-  for the `query` request parameter `page`, the min was unset from `0.00`
-  for the `query` request parameter `size`, the min was unset from `1.00`
-  for the `query` request parameter `page`, the `type/format` was widened from `integer/int32` to `string`
-  for the `query` request parameter `size`, the `type/format` was widened from `integer/int32` to `string`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `500`
-  added `#/components/schemas/ChapterDto` to the `items/items/chapter` response property `allOf` list for the response status `200`
-  the response property `items/items/achievements` became required for the status `200`
-  the response property `items/items/chapter` became required for the status `200`
-  the `items/items/coordinates/x` response's property `format` was narrowed from `none` to `int32` for status `200`
-  the response required property `items/items/creationDate` became not read-only for the status `200`
-  the response required property `items/items/id` became not read-only for the status `200`


### DELETE /space-marines/{id}
- :warning: added the new path request parameter `id`
- :warning: removed the success response with the status `204`
-  api operation id `deleteSpaceMarine` removed and replaced with `delete`
-  api tag `SpaceMarine CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `500`
-  added the success response with the status `200`


### GET /space-marines/{id}
- :warning: added the new path request parameter `id`
- :warning: the response property `coordinates/x` became nullable for the status `200`
- :warning: the response property `coordinates/y` became nullable for the status `200`
- :warning: the response property `coordinates/x` became optional for the status `200`
- :warning: the response property `coordinates/y` became optional for the status `200`
- :warning: the `health` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `coordinates/y` response property's max was unset from `12.00` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `health` response property's min was unset from `0.00` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
- :warning: removed `#/components/schemas/Chapter` from the `chapter` response property `allOf` list for the response status `200`
- :warning: the `chapter` response's property `type` changed from `object` to `any` for status `200`
-  api operation id `getSpaceMarineById` removed and replaced with `getById`
-  api tag `SpaceMarine CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `500`
-  added `#/components/schemas/ChapterDto` to the `chapter` response property `allOf` list for the response status `200`
-  the response property `achievements` became required for the status `200`
-  the response property `chapter` became required for the status `200`
-  the `coordinates/x` response's property `format` was narrowed from `none` to `int32` for status `200`
-  the response required property `creationDate` became not read-only for the status `200`
-  the response required property `id` became not read-only for the status `200`


### PATCH /space-marines/{id}
- :warning: added the new path request parameter `id`
- :warning: the response property `coordinates/x` became nullable for the status `200`
- :warning: the response property `coordinates/y` became nullable for the status `200`
- :warning: the response property `coordinates/x` became optional for the status `200`
- :warning: the response property `coordinates/y` became optional for the status `200`
- :warning: the `health` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `coordinates/y` response property's max was unset from `12.00` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `health` response property's min was unset from `0.00` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
- :warning: removed the request property `achievements`
- :warning: removed the request property `category`
- :warning: removed the request property `chapter`
- :warning: removed the request property `coordinates`
- :warning: removed the request property `health`
- :warning: removed the request property `loyal`
- :warning: removed the request property `name`
- :warning: removed `#/components/schemas/Chapter` from the `chapter` response property `allOf` list for the response status `200`
- :warning: the `chapter` response's property `type` changed from `object` to `any` for status `200`
-  api operation id `patchSpaceMarine` removed and replaced with `patch`
-  api tag `SpaceMarine CRUD` removed
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `422`
-  removed the non-success response with the status `500`
-  added `#/components/schemas/ChapterDto` to the `chapter` response property `allOf` list for the response status `200`
-  the response property `achievements` became required for the status `200`
-  the response property `chapter` became required for the status `200`
-  the `coordinates/x` response's property `format` was narrowed from `none` to `int32` for status `200`
-  the response required property `creationDate` became not read-only for the status `200`
-  the response required property `id` became not read-only for the status `200`


### PUT /space-marines/{id}
- :warning: added the new path request parameter `id`
- :warning: the response property `coordinates/x` became nullable for the status `200`
- :warning: the response property `coordinates/y` became nullable for the status `200`
- :warning: the response property `coordinates/x` became optional for the status `200`
- :warning: the response property `coordinates/y` became optional for the status `200`
- :warning: the `health` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `id` response property's exclusiveMinimum was unset from `true` for the response status `200`
- :warning: the `coordinates/y` response property's max was unset from `12.00` for the response status `200`
- :warning: the `name` response property's minLength was unset from `1` for the response status `200`
- :warning: the `health` response property's min was unset from `0.00` for the response status `200`
- :warning: the `id` response property's min was unset from `0.00` for the response status `200`
- :warning: added `#/components/schemas/AstartesCategory` to the `category` request property `allOf` list
- :warning: added `#/components/schemas/ChapterDto` to the `chapter` request property `allOf` list
- :warning: added `#/components/schemas/CoordinatesDto` to the `coordinates` request property `allOf` list
- :warning: removed the enum value `HELIX` of the request property `category`
- :warning: removed the enum value `SCOUT` of the request property `category`
- :warning: removed the enum value `SUPPRESSOR` of the request property `category`
- :warning: removed the enum value `TACTICAL` of the request property `category`
- :warning: removed the request property `coordinates/x`
- :warning: removed the request property `coordinates/y`
- :warning: removed `#/components/schemas/Chapter` from the `chapter` response property `allOf` list for the response status `200`
- :warning: the `chapter` response's property `type` changed from `object` to `any` for status `200`
-  api operation id `updateSpaceMarine` removed and replaced with `replace`
-  api tag `SpaceMarine CRUD` removed
-  removed `#/components/schemas/Chapter` from the `chapter` request property `allOf` list
-  the request property `category` became nullable
-  the request property `coordinates` became nullable
-  the request property `health` became nullable
-  the request property `loyal` became nullable
-  the request property `name` became nullable
-  the request property `category` became optional
-  the request property `coordinates` became optional
-  the request property `health` became optional
-  the request property `loyal` became optional
-  the request property `name` became optional
-  the `health` request property's exclusiveMinimum was unset from `true`
-  the `name` request property's minLength was unset from `1`
-  the `health` request property's min was unset from `0.00`
-  the `category` request property `type` was widened from `string` to `any`
-  the `chapter` request property `type` was widened from `object` to `any`
-  the `coordinates` request property `type` was widened from `object` to `any`
-  removed the non-success response with the status `400`
-  removed the non-success response with the status `404`
-  removed the non-success response with the status `422`
-  removed the non-success response with the status `500`
-  added `#/components/schemas/ChapterDto` to the `chapter` response property `allOf` list for the response status `200`
-  the response property `achievements` became required for the status `200`
-  the response property `chapter` became required for the status `200`
-  the `coordinates/x` response's property `format` was narrowed from `none` to `int32` for status `200`
-  the response required property `creationDate` became not read-only for the status `200`
-  the response required property `id` became not read-only for the status `200`




## Components
-  removed the schema `Chapter`
-  removed the schema `Coordinates`
-  removed the schema `CountResult`
-  removed the schema `Error`
-  removed the schema `SpaceMarine`
-  removed the schema `SpaceMarineInput`
-  removed the schema `SpaceMarinePage`
-  removed the schema `SpaceMarineUpdate`


## Info
-  a breaking change was detected but the version is still `1.0.0`




