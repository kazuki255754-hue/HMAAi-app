package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecapDao {

    @Query("SELECT * FROM saved_recaps ORDER BY timestamp DESC")
    fun getAllRecaps(): Flow<List<SavedRecapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecap(recap: SavedRecapEntity): Long

    @Query("DELETE FROM saved_recaps WHERE id = :id")
    suspend fun deleteRecapById(id: Long)

    @Query("DELETE FROM saved_recaps")
    suspend fun clearAllRecaps()

    @Query("SELECT * FROM saved_recaps WHERE id = :id")
    suspend fun getRecapById(id: Long): SavedRecapEntity?

    @Query("SELECT COUNT(*) FROM saved_recaps")
    suspend fun getRecapsCount(): Int
}
