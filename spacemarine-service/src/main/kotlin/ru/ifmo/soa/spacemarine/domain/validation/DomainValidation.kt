package ru.ifmo.soa.spacemarine.domain.validation

/**
 * Одно нарушение ограничения целостности.
 *
 * [path] и [message] подобраны так, чтобы строка `"$path: $message"` совпадала с примерами
 * из спецификации дословно — например, `coordinates.y: максимальное значение поля — 12`.
 */
data class ValidationError(
    val path: String,
    val message: String,
) {
    override fun toString(): String = "$path: $message"
}

/**
 * Нарушены ограничения, заданные на уровне класса. Отображается в HTTP 422.
 *
 * Важно, что нарушения собираются **все разом**, а не по первому отказу: спецификация
 * требует вернуть массив `details`, а пользователю полезно увидеть сразу весь список проблем.
 */
class DomainValidationException(
    val violations: List<ValidationError>,
    val className: String = "SpaceMarine",
) : RuntimeException("Нарушены ограничения целостности класса $className") {

    init {
        require(violations.isNotEmpty()) { "Нельзя сообщить о нарушениях, не указав ни одного" }
    }

    val details: List<String> get() = violations.map(ValidationError::toString)
}

/**
 * Накопитель нарушений. Нужен, чтобы правила читались линейно, а не тонули в проверках на null.
 */
class ViolationCollector {
    private val violations = mutableListOf<ValidationError>()

    fun add(path: String, message: String) {
        violations += ValidationError(path, message)
    }

    /** Добавляет нарушение, если [condition] ложно. Возвращает само [condition] для сцепления. */
    fun check(condition: Boolean, path: String, message: String): Boolean {
        if (!condition) add(path, message)
        return condition
    }

    fun failIfAny(className: String = "SpaceMarine") {
        if (violations.isNotEmpty()) throw DomainValidationException(violations.toList(), className)
    }
}
