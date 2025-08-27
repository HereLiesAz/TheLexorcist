package com.hereliesaz.lexorcist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FinancialEntry::class, Case::class, Filter::class, Allegation::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financialEntryDao(): FinancialEntryDao
    abstract fun caseDao(): CaseDao
    abstract fun filterDao(): FilterDao
    abstract fun allegationDao(): AllegationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE financial_entries ADD COLUMN sourceDocument TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE financial_entries ADD COLUMN documentDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `cases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `spreadsheetId` TEXT NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `filters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `name` TEXT NOT NULL, `value` TEXT NOT NULL, FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `allegations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `text` TEXT NOT NULL, FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE TABLE IF NOT EXISTS `financial_entries_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `allegationId` INTEGER, `amount` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `sourceDocument` TEXT NOT NULL, `documentDate` INTEGER NOT NULL, `category` TEXT NOT NULL, FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`allegationId`) REFERENCES `allegations`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                database.execSQL("INSERT INTO `financial_entries_new` (id, caseId, amount, timestamp, sourceDocument, documentDate, category) SELECT id, 0, amount, timestamp, sourceDocument, documentDate, category FROM `financial_entries`")
                database.execSQL("DROP TABLE `financial_entries`")
                database.execSQL("ALTER TABLE `financial_entries_new` RENAME TO `financial_entries`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "legal_parser_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}