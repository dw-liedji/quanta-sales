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
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
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
            Log.i("StockSync", "Successfully synced created stock ${remoteStock.id}")
        } catch (e: Exception) {
            Log.e("StockSync", "Failed to sync created stock ${remoteStock.id}", e)
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
            Log.i("StockSync", "Successfully synced updated stock ${remoteStock.id}")
        } catch (e: Exception) {
            if (e.isNotFoundError()) {
                // Object deleted on server - remove locally
                handleDeletedStock(remoteStock.id, "update")
            } else {
                Log.e("StockSync", "Failed to sync updated stock ${remoteStock.id}", e)
                throw e
            }
        }
    }

    private suspend fun pushDeletedStockAndResolveConflicts(remoteStock: RemoteStock) {
        try {
            remoteDataSource.deleteRemoteStock(remoteStock.orgSlug, remoteStock.id)
            Log.i("StockSync", "Successfully synced deleted stock ${remoteStock.id}")
        } catch (e: Exception) {
            if (e.isNotFoundError()) {
                // Object already deleted on server - remove locally
                handleDeletedStock(remoteStock.id, "delete")
            } else {
                Log.e("StockSync", "Failed to sync deleted stock ${remoteStock.id}", e)
                throw e
            }
        }
    }

    // --- Handle deleted stock (404 scenario) ---
    private suspend fun handleDeletedStock(stockId: String, operationType: String) {
        try {
            // Remove from local database
            localDataSource.deleteLocalStockById(stockId)

            // Log the cleanup action
            Log.i("StockSync", "Stock $stockId was deleted on server during $operationType, removed locally")

            // You could also emit an event here for UI cleanup
            // eventBus.post(StockDeletedEvent(stockId))

        } catch (e: Exception) {
            Log.e("StockSync", "Failed to clean up locally deleted stock $stockId", e)
            // Don't throw - we want to consider the sync successful since the object is gone
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
                else -> {
                    Log.w("StockSync", "Unsupported operation type: ${currentOperation.operationType}")
                }
            }

            // Delete the completed operation
            pendingOperationDao.deleteByKeys(
                entityType = currentOperation.entityType,
                entityId = currentOperation.entityId,
                operationType = currentOperation.operationType,
                orgId = currentOperation.orgId
            )

            // If other pending operations remain, status is still pending
            val newStatus = if ((totalPending - 1) > 0) SyncStatus.PENDING else SyncStatus.SYNCED
            localDataSource.updateSyncStatus(currentOperation.entityId, newStatus)

            Log.i("StockSyncOperation", "Successfully processed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}")

        } catch (e: Exception) {
            // Don't increment failure count for 404 errors (they're handled successfully)
            if (!e.isNotFoundError()) {
                pendingOperationDao.incrementFailureCount(
                    entityType = currentOperation.entityType,
                    entityId = currentOperation.entityId,
                    operationType = currentOperation.operationType,
                    orgId = currentOperation.orgId
                )

                // Get updated failure count to decide status
                val updatedFailureCount = pendingOperationDao.getFailureCount(
                    entityType = currentOperation.entityType,
                    entityId = currentOperation.entityId,
                    operationType = currentOperation.operationType,
                    orgId = currentOperation.orgId
                )

                val status = if (updatedFailureCount > 5) SyncStatus.FAILED else SyncStatus.PENDING
                localDataSource.updateSyncStatus(currentOperation.entityId, status)

                Log.e("StockSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)
            }
            // For 404 errors, we don't log as failures since they're handled gracefully
        }
    }

    override fun getEntity(): PendingOperationEntityType {
        return PendingOperationEntityType.Session
    }

    override suspend fun pullAll(organization: String) {
        Log.i("StockSync", "Full sync started")

        try {
            val remoteSessions = remoteDataSource.getRemoteStocks(organization)

            val domainSessions = remoteSessions.map { stockMapper.mapRemoteToDomain(it) }
            val localSessions = domainSessions.map { stockMapper.mapDomainToLocal(it) }

            localDataSource.saveLocalStocks(localSessions)
            Log.i("StockSync", "Full sync completed: ${localSessions.size} stocks")
        } catch (e: Exception) {
            Log.e("StockSync", "Full sync failed: ${e.message}", e)
            throw e
        }
    }

    // --- Extension for 404 detection ---
    private fun Exception.isNotFoundError(): Boolean {
        return (this as? HttpException)?.code() == HTTP_NOT_FOUND
    }
}