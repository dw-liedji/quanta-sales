package com.datavite.eat.data.local.datasource

import com.datavite.eat.data.local.model.LocalTeachingPeriod
import kotlinx.coroutines.flow.Flow

interface TeachingPeriodLocalDataSource {
    suspend fun getTeachingPeriodsFlow(): Flow<List<LocalTeachingPeriod>>
    suspend fun getAllTeachingPeriods(): List<LocalTeachingPeriod>
    suspend fun saveTeachingPeriods(localPeriods: List<LocalTeachingPeriod>)
    suspend fun getUnSyncedPeriods(): List<LocalTeachingPeriod>
    suspend fun markAsSynced(period: LocalTeachingPeriod)
    suspend fun saveTeachingPeriod(period: LocalTeachingPeriod)
    suspend fun deleteTeachingPeriod(periodId: String)
    suspend fun markAsPendingDeletion(periodId: String)
}