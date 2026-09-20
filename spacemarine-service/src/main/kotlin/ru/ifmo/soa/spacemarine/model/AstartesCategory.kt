package ru.ifmo.soa.spacemarine.model

/** Значения и порядок зафиксированы схемой `AstartesCategory` в спецификации. */
enum class AstartesCategory {
    SCOUT,
    SUPPRESSOR,
    TACTICAL,
    HELIX,
    ;

    companion object {
        fun byNameOrNull(name: String): AstartesCategory? = entries.firstOrNull { it.name == name }
    }
}
