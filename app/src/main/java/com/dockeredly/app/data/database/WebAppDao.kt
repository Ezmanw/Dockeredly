package com.dockeredly.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {
    @Query("SELECT * FROM web_apps ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<WebAppEntity>>

    @Query("SELECT * FROM web_apps WHERE id = :id")
    fun observeById(id: String): Flow<WebAppEntity?>

    @Query("SELECT * FROM web_apps WHERE id = :id")
    suspend fun getById(id: String): WebAppEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM web_apps")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WebAppEntity)

    @Update
    suspend fun update(entity: WebAppEntity)

    @Delete
    suspend fun delete(entity: WebAppEntity)

    @Query("DELETE FROM web_apps")
    suspend fun deleteAll()

    @Query("UPDATE web_apps SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    @Query("UPDATE web_apps SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
