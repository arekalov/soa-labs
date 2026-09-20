package ru.ifmo.soa.starship.repository

import ru.ifmo.soa.starship.model.Starship

/**
 * Вставка корабля с заданным идентификатором.
 *
 * Штатный `save` на сущности с непустым `id` уходит в `merge` и молча перезаписал бы
 * чужую строку. Фрагмент выполняет именно `persist`, поэтому дубликат ключа приводит
 * к нарушению целостности, а не к потере данных.
 */
interface StarshipInsert {
    fun insert(starship: Starship): Starship
}
