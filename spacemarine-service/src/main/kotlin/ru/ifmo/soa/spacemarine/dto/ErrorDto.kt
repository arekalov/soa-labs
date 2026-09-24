package ru.ifmo.soa.spacemarine.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)
