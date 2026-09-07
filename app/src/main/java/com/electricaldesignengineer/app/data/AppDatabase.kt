package com.electricaldesignengineer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        CableCatalogEntity::class,
        BreakerCatalogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    abstract fun catalogDao(): CatalogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "electrical_design_engineer.db"
                )
                    /*
                     * The project is still in the engineering-core
                     * development stage.
                     *
                     * Existing database contents may be recreated
                     * when the schema changes.
                     *
                     * Before production release, replace this with
                     * explicit Room migrations to preserve project data.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
