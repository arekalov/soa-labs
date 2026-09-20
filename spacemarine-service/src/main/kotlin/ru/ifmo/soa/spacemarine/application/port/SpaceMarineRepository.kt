package ru.ifmo.soa.spacemarine.application.port

import ru.ifmo.soa.spacemarine.application.query.Page
import ru.ifmo.soa.spacemarine.application.query.Paging
import ru.ifmo.soa.spacemarine.application.query.SpaceMarineQuery
import ru.ifmo.soa.spacemarine.domain.model.SpaceMarine

/**
 * Порт хранилища десантников.
 *
 * Интерфейс объявлен во внутреннем слое, а реализация живёт в адаптере — это инверсия
 * зависимости, благодаря которой прикладной слой ничего не знает ни о JPA, ни о СУБД.
 */
interface SpaceMarineRepository {

    fun findById(id: Int): SpaceMarine?

    /** Сохраняет нового десантника и возвращает его с присвоенным идентификатором. */
    fun create(marine: SpaceMarine): SpaceMarine

    fun update(marine: SpaceMarine): SpaceMarine

    /** @return `true`, если элемент существовал и был удалён. */
    fun deleteById(id: Int): Boolean

    /** Выборка с фильтрами, сортировкой и пагинацией. */
    fun search(query: SpaceMarineQuery): Page<SpaceMarine>

    /**
     * Количество десантников заданного ордена.
     *
     * [parentLegion] `null` означает «любой легион»: спецификация помечает параметр
     * необязательным и не описывает семантику его отсутствия.
     */
    fun countByChapter(name: String, parentLegion: String?): Long

    /** Количество десантников, у которых `health` строго больше порога. */
    fun countByHealthGreaterThan(threshold: Float): Long

    /** Страница десантников, чьё имя начинается с подстроки; упорядочены по `id`. */
    fun findByNamePrefix(prefix: String, paging: Paging): Page<SpaceMarine>
}
