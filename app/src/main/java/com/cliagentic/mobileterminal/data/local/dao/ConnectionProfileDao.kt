package com.cliagentic.mobileterminal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cliagentic.mobileterminal.data.local.entity.ConnectionProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionProfileDao {
    @Query("SELECT * FROM connection_profiles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ConnectionProfileEntity>>

    @Query("SELECT * FROM connection_profiles WHERE id = :id")
    fun observeById(id: Long): Flow<ConnectionProfileEntity?>

    @Query("SELECT * FROM connection_profiles WHERE id = :id")
    suspend fun getById(id: Long): ConnectionProfileEntity?

    @Query("SELECT * FROM connection_profiles ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ConnectionProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionProfileEntity): Long

    @Update
    suspend fun update(entity: ConnectionProfileEntity)

    @Delete
    suspend fun delete(entity: ConnectionProfileEntity)
}
