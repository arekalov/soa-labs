package ru.ifmo.soa.spacemarine.api

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
