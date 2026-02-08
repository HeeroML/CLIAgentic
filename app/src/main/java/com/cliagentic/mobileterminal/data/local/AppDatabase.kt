package com.cliagentic.mobileterminal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cliagentic.mobileterminal.data.local.dao.ConnectionProfileDao
import com.cliagentic.mobileterminal.data.local.dao.KnownHostDao
import com.cliagentic.mobileterminal.data.local.entity.ConnectionProfileEntity
import com.cliagentic.mobileterminal.data.local.entity.KnownHostEntity

@Database(
    entities = [ConnectionProfileEntity::class, KnownHostEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionProfileDao(): ConnectionProfileDao
    abstract fun knownHostDao(): KnownHostDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE connection_profiles ADD COLUMN ptyType TEXT NOT NULL DEFAULT 'xterm-256color'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Clean break for host trust state: switch from fingerprint-only to raw-key records.
                db.execSQL("DROP TABLE IF EXISTS known_hosts")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS known_hosts (
                        host TEXT NOT NULL,
                        port INTEGER NOT NULL,
                        algorithm TEXT NOT NULL,
                        hostKey BLOB NOT NULL,
                        sha256Fingerprint TEXT NOT NULL,
                        md5Fingerprint TEXT NOT NULL,
                        trustedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(host, port)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
