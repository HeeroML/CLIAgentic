package com.cliagentic.mobileterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cliagentic.mobileterminal.data.local.dao.ConnectionProfileDao
import com.cliagentic.mobileterminal.data.local.dao.KnownHostDao
import com.cliagentic.mobileterminal.data.local.entity.ConnectionProfileEntity
import com.cliagentic.mobileterminal.data.local.entity.KnownHostEntity

@Database(
    entities = [ConnectionProfileEntity::class, KnownHostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionProfileDao(): ConnectionProfileDao
    abstract fun knownHostDao(): KnownHostDao
}
