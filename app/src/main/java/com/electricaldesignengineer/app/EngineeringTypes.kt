package com.electricaldesignengineer.app

/**
 * Shared engineering domain types.
 *
 * These types intentionally do not belong to a calculation engine.
 * Catalogs, calculation cores and UI layers can use them without creating
 * a dependency on a legacy calculator implementation.
 */
enum class CableMaterial {
    COPPER,
    ALUMINIUM
}

enum class InsulationType {
    PVC,
    XLPE,
    EPR
}

enum class InstallationMethod {
    CONDUIT,
    TRUNKING,
    CABLE_TRAY,
    CABLE_LADDER,
    FREE_AIR,
    DIRECT_BURIED,
    DUCT
}
