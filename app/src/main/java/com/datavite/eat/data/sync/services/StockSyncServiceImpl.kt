package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteStock
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.StockLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.StockMapper
import com.datavite.eat.data.remote.datasource.StockRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import javax.inject.Inject

class StockSyncServiceImpl @Inject constructor(
    private val remoteDataSource: StockRemoteDataSource,
    private val localDataSource: StockLocalDataSource,
    private val stockMapper: StockMapper,
    private val pendingOperationDao: PendingOperationDao,
) : StockSyncService {

    private suspend fun pushCreatedStockAndResolveConflicts(remoteStock: RemoteStock) {
        try {

            remoteDataSource.createRemoteStock(remoteStock.orgSlug, remoteStock)
            val updatedSession = stockMapper.mapRemoteToDomain(remoteStock)

            // Must implement conflict handling
            val syncedLocal = stockMapper.mapDomainToLocal(updatedSession)
            localDataSource.insertLocalStock(syncedLocal)
            Log.e("StockSync", "Success to sync created session ${remoteStock.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StockSync", "Failed to sync created session ${remoteStock.id}", e)
            throw e
        }
    }


    private suspend fun pushUpdatedStockAndResolveConflicts(remoteStock: RemoteStock) {
        try {

            remoteDataSource.updateRemoteStock(remoteStock.orgSlug, remoteStock)
            val updatedSession = stockMapper.mapRemoteToDomain(remoteStock)

            // Must implement conflict handling
            val syncedLocal = stockMapper.mapDomainToLocal(updatedSession)
            localDataSource.insertLocalStock(syncedLocal)
            Log.e("StockSync", "Success to sync updated session ${remoteStock.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StockSync", "Failed to sync updated session ${remoteStock.id}", e)
            throw e        }
    }





    private suspend fun pushDeletedStockAndResolveConflicts(remoteStock: RemoteStock) {
        try {
            remoteDataSource.deleteRemoteStock(remoteStock.orgSlug, remoteStock.id)
            Log.e("StockSync", "Success to sync deleted session ${remoteStock.id}", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StockSync", "Failed to sync deleted session ${remoteStock.id}", e)
            throw e
        }
    }

    override suspend fun push(operations: List<PendingOperation>) {
        for (operation in operations) {
            syncOperation(operation, operations)
        }
    }

    override suspend fun hasCachedData(): Boolean {
        return localDataSource.getLocalStockCount() != 0
    }

    private suspend fun syncOperation(currentOperation: PendingOperation, allOperations: List<PendingOperation>) {
        val session = currentOperation.parsePayload<RemoteStock>()

        // Count all pending operations for this entity
        val totalPending = allOperations.count { it.entityId == currentOperation.entityId }

        // Mark entity as syncing before pushing changes
        localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCING)

        try {
            when (currentOperation.operationType) {
                PendingOperationType.CREATE -> pushCreatedStockAndResolveConflicts(session)
                PendingOperationType.UPDATE -> pushUpdatedStockAndResolveConflicts(session)
                PendingOperationType.START_SESSION -> {}
                PendingOperationType.END_SESSION -> {}
                PendingOperationType.APPROVE_SESSION -> {}
                PendingOperationType.DELETE -> pushDeletedStockAndResolveConflicts(session)
            }

            // Delete the completed operation
            pendingOperationDao.deleteByKeys(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)

            // If other pending operations remain, status is still pending
            if ((totalPending - 1) > 0) {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.PENDING)
            } else {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCED)
            }
            Log.i("StockSyncOperation", "Success to StockSyncOperation operation  ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}")

        } catch (e: Exception) {
            // Increment failure count
            pendingOperationDao.incrementFailureCount(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)

            // Get updated failure count to decide status
            val updatedFailureCount = pendingOperationDao.getFailureCount(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)

            if (updatedFailureCount > 5) {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.FAILED)
            } else {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.PENDING)
            }
            Log.e("BillingSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)

            e.printStackTrace()
        }

    }

    override fun getEntity(): PendingOperationEntityType {
        return PendingOperationEntityType.Session
    }

    override suspend fun pullAll(organization: String) {
        Log.e("StockSync", "Full sync started", )

        try {
            val remoteSessions = remoteDataSource.getRemoteStocks(organization)

            val domainSessions = remoteSessions.map { stockMapper.mapRemoteToDomain(it) }
            val localSessions = domainSessions.map { stockMapper.mapDomainToLocal(it) }
            //localDataSource.clear()
            localDataSource.saveLocalStocks(localSessions)
            Log.e("StockSync", "Full sync success ${localSessions.size} sessions", )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StockSync", "Full sync failed: ${e.message}", e)
            throw e
        }
    }
}
