package ru.ifmo.soa.starship.dto

/**
 * Тело создания и переименования. Схема `StarshipInput`. Проверку делает бизнес-компонент.
 *
 * Обычный класс с var, а не data class с val: иначе Jackson берёт сгенерированный
 * конструктор без аргументов и оставляет поле пустым — так же устроен SpaceMarineInputDto.
 */
class StarshipInputDto(
    var name: String? = null,
) : java.io.Serializable
