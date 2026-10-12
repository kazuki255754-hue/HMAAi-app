package com.example.data.local

import kotlinx.coroutines.flow.Flow

class RecapRepository(private val recapDao: RecapDao) {

    val allRecaps: Flow<List<SavedRecapEntity>> = recapDao.getAllRecaps()

    suspend fun insert(recap: SavedRecapEntity): Long = recapDao.insertRecap(recap)

    suspend fun delete(id: Long) = recapDao.deleteRecapById(id)

    suspend fun clearAll() = recapDao.clearAllRecaps()

    suspend fun getById(id: Long): SavedRecapEntity? = recapDao.getRecapById(id)

    suspend fun getCount(): Int = recapDao.getRecapsCount()
}
