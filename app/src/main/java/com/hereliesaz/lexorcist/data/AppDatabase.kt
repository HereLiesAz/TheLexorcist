package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hereliesaz.lexorcist.data.Evidence // Corrected import

@Database(entities = [Evidence::class, Case::class, Filter::class], version = 1, exportSchema = false)
@Database(entities = [Evidence::class, Case::class, Allegation::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun evidenceDao(): EvidenceDao
    abstract fun caseDao(): CaseDao
    abstract fun filterDao(): FilterDao
    abstract fun allegationDao(): AllegationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lexorcist_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
