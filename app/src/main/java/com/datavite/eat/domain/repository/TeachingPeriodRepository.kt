package com.datavite.eat.domain.repository

import com.datavite.eat.domain.model.DomainTeachingPeriod
import kotlinx.coroutines.flow.Flow

interface TeachingPeriodRepository {
    suspend fun getTeachingPeriodsFlow(): Flow<List<DomainTeachingPeriod>>
    suspend fun getAllTeachingPeriods(): List<DomainTeachingPeriod>
    suspend fun createTeachingPeriod(organization: String, period: DomainTeachingPeriod)
    suspend fun updateTeachingPeriod(organization: String, period: DomainTeachingPeriod)
    suspend fun deleteTeachingPeriod(organization: String, periodId: String)
    suspend fun syncTeachingPeriods(organization: String)
}