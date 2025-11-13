package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteTeachingSession
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.TeachingSessionLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.TeachingSessionMapper
import com.datavite.eat.data.remote.datasource.TeachingSessionRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import javax.inject.Inject

class TeachingSessionSyncServiceImpl @Inject constructor(
    private val remoteDataSource: TeachingSessionRemoteDataSource,
    private val localDataSource: TeachingSessionLocalDataSource,
    private val teachingSessionMapper: TeachingSessionMapper,
    private val pendingOperationDao: PendingOperationDao,
) : TeachingSessionSyncService {

    private suspend fun pushCreatedTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {

            remoteDataSource.createRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession)
            val updatedSession = teachingSessionMapper.mapRemoteToDomain(remoteTeachingSession)

            // Must implement conflict handling
            val syncedLocal = teachingSessionMapper.mapDomainToLocal(updatedSession)
            localDataSource.saveLocalTeachingSession(syncedLocal)
            Log.e("TeachingSessionSync", "Success to sync created session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync created session ${remoteTeachingSession.id}", e)
            throw e
        }
    }


    private suspend fun pushUpdatedTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {

            remoteDataSource.updateRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession)
            val updatedSession = teachingSessionMapper.mapRemoteToDomain(remoteTeachingSession)

            // Must implement conflict handling
            val syncedLocal = teachingSessionMapper.mapDomainToLocal(updatedSession)
            localDataSource.saveLocalTeachingSession(syncedLocal)
            Log.e("TeachingSessionSync", "Success to sync updated session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync updated session ${remoteTeachingSession.id}", e)
            throw e        }
    }

    private suspend fun pushStartedTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {

            remoteDataSource.startRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession)
            val updatedSession = teachingSessionMapper.mapRemoteToDomain(remoteTeachingSession)

            // Must implement conflict handling
            val syncedLocal = teachingSessionMapper.mapDomainToLocal(updatedSession)
            localDataSource.saveLocalTeachingSession(syncedLocal)
            Log.e("TeachingSessionSync", "Success to sync updated session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync updated session ${remoteTeachingSession.id}", e)
            throw e        }
    }


    private suspend fun pushEndedTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {

            remoteDataSource.endRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession)
            val updatedSession = teachingSessionMapper.mapRemoteToDomain(remoteTeachingSession)

            // Must implement conflict handling
            val syncedLocal = teachingSessionMapper.mapDomainToLocal(updatedSession)
            localDataSource.saveLocalTeachingSession(syncedLocal)
            Log.e("TeachingSessionSync", "Success to sync updated session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync updated session ${remoteTeachingSession.id}", e)
            throw e        }
    }


    private suspend fun pushApproveTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {

            remoteDataSource.approveRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession)
            val updatedSession = teachingSessionMapper.mapRemoteToDomain(remoteTeachingSession)

            // Must implement conflict handling
            val syncedLocal = teachingSessionMapper.mapDomainToLocal(updatedSession)
            localDataSource.saveLocalTeachingSession(syncedLocal)
            Log.e("TeachingSessionSync", "Success to sync updated session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync updated session ${remoteTeachingSession.id}", e)
            throw e        }
    }

    private suspend fun pushDeletedTeachingSessionAndResolveConflicts(remoteTeachingSession: RemoteTeachingSession) {
        try {
            remoteDataSource.deleteRemoteTeachingSession(remoteTeachingSession.orgSlug, remoteTeachingSession.id)
            Log.e("TeachingSessionSync", "Success to sync deleted session ${remoteTeachingSession.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Failed to sync deleted session ${remoteTeachingSession.id}", e)
            throw e
        }
    }

    override suspend fun push(operations: List<PendingOperation>) {
        for (operation in operations) {
            syncOperation(operation, operations)
        }
    }

    override suspend fun hasCachedData(): Boolean {
        return localDataSource.getLocalTeachingSessionCount() != 0
    }

    private suspend fun syncOperation(currentOperation: PendingOperation, allOperations: List<PendingOperation>) {
        val session = currentOperation.parsePayload<RemoteTeachingSession>()

        // Count all pending operations for this entity
        val totalPending = allOperations.count { it.entityId == currentOperation.entityId }

        // Mark entity as syncing before pushing changes
        localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCING)

        try {
            when (currentOperation.operationType) {
                PendingOperationType.CREATE -> pushCreatedTeachingSessionAndResolveConflicts(session)
                PendingOperationType.UPDATE -> pushUpdatedTeachingSessionAndResolveConflicts(session)
                PendingOperationType.START_SESSION -> pushStartedTeachingSessionAndResolveConflicts(session)
                PendingOperationType.END_SESSION -> pushEndedTeachingSessionAndResolveConflicts(session)
                PendingOperationType.APPROVE_SESSION -> pushApproveTeachingSessionAndResolveConflicts(session)
                PendingOperationType.DELETE -> pushDeletedTeachingSessionAndResolveConflicts(session)
            }

            // Delete the completed operation
            pendingOperationDao.deleteById(currentOperation.id)

            // If other pending operations remain, status is still pending
            if ((totalPending - 1) > 0) {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.PENDING)
            } else {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCED)
            }
            Log.i("TeachingSessionSyncOperation", "Success to TeachingSessionSyncOperation operation ${currentOperation.id}")

        } catch (e: Exception) {
            // Increment failure count
            pendingOperationDao.incrementFailureCount(currentOperation.id)

            // Get updated failure count to decide status
            val updatedFailureCount = pendingOperationDao.getFailureCount(currentOperation.id)

            if (updatedFailureCount > 5) {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.FAILED)
            } else {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.PENDING)
            }
            Log.e("TeachingSessionSyncOperation", "Failed to TeachingSessionSyncOperation operation ${currentOperation.id}")

            e.printStackTrace()
        }

    }

    override fun getEntity(): PendingOperationEntityType {
        return PendingOperationEntityType.Session
    }

    override suspend fun pullAll(organization: String) {
        Log.e("TeachingSessionSync", "Full sync started", )

        try {
            val remoteSessions = remoteDataSource.getRemoteTeachingSessions(organization)

            val domainSessions = remoteSessions.map { teachingSessionMapper.mapRemoteToDomain(it) }
            val localSessions = domainSessions.map { teachingSessionMapper.mapDomainToLocal(it) }
            //localDataSource.clear()
            localDataSource.saveLocalTeachingSessions(localSessions)
            Log.e("TeachingSessionSync", "Full sync success ${localSessions.size} sessions", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TeachingSessionSync", "Full sync failed: ${e.message}", e)
            throw e
        }
    }
}
