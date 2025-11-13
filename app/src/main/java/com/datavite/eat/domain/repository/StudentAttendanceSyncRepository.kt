package com.datavite.eat.domain.repository

interface StudentAttendanceSyncRepository {

    suspend fun syncCreatedAttendances(organization: String)

    suspend fun syncUpdatedAttendances(organization: String)

    suspend fun syncDeletedAttendances(organization: String)

    suspend fun syncAllAttendances(organization: String)

    suspend fun fullSyncAllAttendances(organization: String)

}