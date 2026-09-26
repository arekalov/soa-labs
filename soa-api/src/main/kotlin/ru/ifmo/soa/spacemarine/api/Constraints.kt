package ru.ifmo.soa.spacemarine.api

import java.time.Instant
import java.time.temporal.ChronoUnit

const val MAX_COORDINATE_Y = 12.0

/** Postgres хранит микросекунды, клиент присылает миллисекунды: без среза фильтр по creationDate не совпадёт. */
val TIME_PRECISION: ChronoUnit = ChronoUnit.MILLIS

fun nowTruncated(): Instant = Instant.now().truncatedTo(TIME_PRECISION)
