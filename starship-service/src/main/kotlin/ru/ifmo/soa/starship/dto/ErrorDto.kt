package ru.ifmo.soa.starship.dto

import com.fasterxml.jackson.annotation.JsonInclude

/** Схема `Error`. Совпадает с форматом первого сервиса. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)
