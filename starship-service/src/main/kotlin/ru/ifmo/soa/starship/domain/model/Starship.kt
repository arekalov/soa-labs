package ru.ifmo.soa.starship.domain.model

/**
 * Десантный корабль.
 *
 * Идентификатор либо задаётся клиентом (эндпоинт `/create/{id}/{name}` из спецификации ЛР1),
 * либо выдаётся хранилищем — в обоих случаях он известен до создания объекта.
 *
 * [marines] хранит идентификаторы десантников из первого сервиса. Полноценных объектов
 * здесь нет и быть не должно: это граница сервисов, связь поддерживается только по REST.
 */
data class Starship(
    val id: Long,
    val name: String,
    val marines: Set<Int> = emptySet(),
) {
    init {
        require(id > 0) { "Идентификатор корабля должен быть больше 0" }
        require(name.isNotBlank()) { "Название корабля не может быть пустым" }
        require(marines.all { it > 0 }) { "Идентификатор десантника должен быть больше 0" }
    }

    fun hasOnBoard(spaceMarineId: Int): Boolean = spaceMarineId in marines

    /** Сажает десантника на борт. Существование десантника проверяет вызывающий сценарий. */
    fun board(spaceMarineId: Int): Starship = copy(marines = marines + spaceMarineId)

    /** Снимает десантника с борта. Проверку присутствия выполняет вызывающий сценарий. */
    fun unload(spaceMarineId: Int): Starship = copy(marines = marines - spaceMarineId)

    fun rename(newName: String): Starship = copy(name = newName)
}
