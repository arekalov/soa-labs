package ru.ifmo.soa.spacemarine.dto

import com.fasterxml.jackson.annotation.JsonInclude

/** Схема `Error`. `details` не сериализуется при отсутствии: в схеме поле необязательное. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)
