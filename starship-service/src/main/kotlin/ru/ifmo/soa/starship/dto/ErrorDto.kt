package ru.ifmo.soa.starship.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: Int,
    val message: String,
    val details: List<String>? = null,
)
