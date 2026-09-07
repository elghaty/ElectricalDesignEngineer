package com.electricaldesignengineer.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    // ============================================================
    // CABLE CATALOG
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCable(
        cable: CableCatalogEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCables(
        cables: List<CableCatalogEntity>
    )

    @Delete
    suspend fun deleteCable(
        cable: CableCatalogEntity
    )

    @Query("DELETE FROM cable_catalog")
    suspend fun deleteAllCables()

    @Query(
        """
        SELECT *
        FROM cable_catalog
        ORDER BY manufacturerName, productFamily, sizeMm2
        """
    )
    fun observeAllCables(): Flow<List<CableCatalogEntity>>

    @Query(
        """
        SELECT *
        FROM cable_catalog
        WHERE verified = 1
        ORDER BY manufacturerName, productFamily, sizeMm2
        """
    )
    fun observeVerifiedCables(): Flow<List<CableCatalogEntity>>

    @Query(
        """
        SELECT *
        FROM cable_catalog
        WHERE verified = 1
          AND material = :material
          AND insulation = :insulation
          AND installationMethod = :installationMethod
        ORDER BY sizeMm2
        """
    )
    suspend fun getVerifiedCables(
        material: String,
        insulation: String,
        installationMethod: String
    ): List<CableCatalogEntity>

    @Query(
        """
        SELECT *
        FROM cable_catalog
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getCableById(
        id: String
    ): CableCatalogEntity?

    @Query(
        """
        SELECT *
        FROM cable_catalog
        WHERE manufacturerId = :manufacturerId
          AND verified = 1
        ORDER BY productFamily, sizeMm2
        """
    )
    suspend fun getVerifiedCablesByManufacturer(
        manufacturerId: String
    ): List<CableCatalogEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM cable_catalog
        """
    )
    suspend fun cableCount(): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM cable_catalog
        WHERE verified = 1
        """
    )
    suspend fun verifiedCableCount(): Int


    // ============================================================
    // BREAKER CATALOG
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreaker(
        breaker: BreakerCatalogEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakers(
        breakers: List<BreakerCatalogEntity>
    )

    @Delete
    suspend fun deleteBreaker(
        breaker: BreakerCatalogEntity
    )

    @Query("DELETE FROM breaker_catalog")
    suspend fun deleteAllBreakers()

    @Query(
        """
        SELECT *
        FROM breaker_catalog
        ORDER BY manufacturerName, productFamily, ratedCurrentA
        """
    )
    fun observeAllBreakers(): Flow<List<BreakerCatalogEntity>>

    @Query(
        """
        SELECT *
        FROM breaker_catalog
        WHERE verified = 1
        ORDER BY manufacturerName, productFamily, ratedCurrentA
        """
    )
    fun observeVerifiedBreakers(): Flow<List<BreakerCatalogEntity>>

    @Query(
        """
        SELECT *
        FROM breaker_catalog
        WHERE verified = 1
          AND poles = :poles
          AND ratedVoltageV >= :requiredVoltageV
          AND ratedCurrentA >= :minimumCurrentA
          AND icuKA >= :minimumIcuKA
        ORDER BY ratedCurrentA, icuKA
        """
    )
    suspend fun getVerifiedBreakers(
        poles: Int,
        requiredVoltageV: Double,
        minimumCurrentA: Double,
        minimumIcuKA: Double
    ): List<BreakerCatalogEntity>

    @Query(
        """
        SELECT *
        FROM breaker_catalog
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getBreakerById(
        id: String
    ): BreakerCatalogEntity?

    @Query(
        """
        SELECT *
        FROM breaker_catalog
        WHERE manufacturerId = :manufacturerId
          AND verified = 1
        ORDER BY productFamily, ratedCurrentA
        """
    )
    suspend fun getVerifiedBreakersByManufacturer(
        manufacturerId: String
    ): List<BreakerCatalogEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM breaker_catalog
        """
    )
    suspend fun breakerCount(): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM breaker_catalog
        WHERE verified = 1
        """
    )
    suspend fun verifiedBreakerCount(): Int
}
