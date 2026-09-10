package com.electricaldesignengineer.app

/**
 * Central engineering-standard registry.
 *
 * This file contains standards references only.
 * It does not perform engineering calculations.
 *
 * The ProfessionalEngineeringCore is the single calculation core.
 */
object EngineeringStandards {

    data class StandardReference(
        val code: String,
        val title: String,
        val edition: String,
        val scope: String,
        val authority: String = "IEC"
    )

    val cableSelection = StandardReference(
        code = "IEC 60364-5-52",
        title = "Low-voltage electrical installations - Wiring systems",
        edition = "2009 + AMD1:2024",
        scope = "Selection and erection of wiring systems, conductor sizing, installation methods and voltage drop"
    )

    val shortCircuit = StandardReference(
        code = "IEC 60909-0",
        title = "Short-circuit currents in three-phase AC systems - Calculation of currents",
        edition = "2026",
        scope = "Calculation of short-circuit currents in three-phase AC systems"
    )

    val circuitBreakers = StandardReference(
        code = "IEC 60947-2",
        title = "Low-voltage switchgear and controlgear - Circuit-breakers",
        edition = "2024",
        scope = "LV circuit-breaker ratings, characteristics and verification"
    )

    val assemblies = StandardReference(
        code = "IEC 61439-1",
        title = "Low-voltage switchgear and controlgear assemblies - General rules",
        edition = "2020",
        scope = "LV assembly characteristics, construction and verification"
    )

    val powerAssemblies = StandardReference(
        code = "IEC 61439-2",
        title = "Low-voltage switchgear and controlgear assemblies - Power switchgear and controlgear assemblies",
        edition = "2020",
        scope = "Power switchgear and controlgear assemblies"
    )

    val earthing = StandardReference(
        code = "IEC 60364-5-54",
        title = "Low-voltage electrical installations - Earthing arrangements and protective conductors",
        edition = "Project standard basis",
        scope = "Earthing arrangements, protective conductors and bonding"
    )

    val transformer = StandardReference(
        code = "IEC 60076",
        title = "Power transformers",
        edition = "Applicable current edition",
        scope = "Power transformer ratings, characteristics and performance"
    )

    val all: List<StandardReference> = listOf(
        cableSelection,
        shortCircuit,
        circuitBreakers,
        assemblies,
        powerAssemblies,
        earthing,
        transformer
    )
}
