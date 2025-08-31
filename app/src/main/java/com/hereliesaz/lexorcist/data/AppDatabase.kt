package com.hereliesaz.lexorcist.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Case::class, Evidence::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun evidenceDao(): EvidenceDao
}
