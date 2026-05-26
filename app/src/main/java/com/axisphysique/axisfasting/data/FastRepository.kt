package com.axisphysique.axisfasting.data

import kotlinx.coroutines.flow.Flow

class FastRepository(private val fastDao: FastDao) {
    val allFasts: Flow<List<FastEntity>> = fastDao.getAllFasts()
    val fastCount: Flow<Int> = fastDao.getFastCount()

    suspend fun insertFast(fast: FastEntity) {
        fastDao.insertFast(fast)
    }

    suspend fun deleteFast(id: Int) {
        fastDao.deleteFast(id)
    }

    suspend fun deleteAllFasts() {
        fastDao.deleteAllFasts()
    }
}
