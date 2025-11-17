package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.TransactionLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.TransactionMapper
import com.datavite.eat.data.remote.datasource.TransactionRemoteDataSource
import com.datavite.eat.data.remote.model.RemoteTransaction
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import javax.inject.Inject

class TransactionSyncServiceImpl @Inject constructor(
    private val remoteDataSource: TransactionRemoteDataSource,
    private val localDataSource: TransactionLocalDataSource,
    private val transactionMapper: TransactionMapper,
    private val pendingOperationDao: PendingOperationDao,
) : TransactionSyncService {

    private suspend fun pushCreatedTransactionAndResolveConflicts(remoteTransaction: RemoteTransaction) {
        try {
            remoteDataSource.createRemoteTransaction(remoteTransaction.orgSlug, remoteTransaction)
            val updatedTransaction = transactionMapper.mapRemoteToDomain(remoteTransaction)

            // Must implement conflict handling
            val syncedLocal = transactionMapper.mapDomainToLocal(updatedTransaction)
            localDataSource.insertLocalTransaction(syncedLocal)
            Log.i("TransactionSync", "Successfully synced created transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            Log.e("TransactionSync", "Failed to sync created transaction ${remoteTransaction.id}", e)
            throw e
        }
    }

    private suspend fun pushUpdatedTransactionAndResolveConflicts(remoteTransaction: RemoteTransaction) {
        try {
            remoteDataSource.updateRemoteTransaction(remoteTransaction.orgSlug, remoteTransaction)
            val updatedTransaction = transactionMapper.mapRemoteToDomain(remoteTransaction)

            // Must implement conflict handling
            val syncedLocal = transactionMapper.mapDomainToLocal(updatedTransaction)
            localDataSource.insertLocalTransaction(syncedLocal)
            Log.i("TransactionSync", "Successfully synced updated transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            if (e.isNotFoundError()) {
                // Object deleted on server - remove locally
                handleDeletedTransaction(remoteTransaction.id, "update")
            } else {
                Log.e("TransactionSync", "Failed to sync updated transaction ${remoteTransaction.id}", e)
                throw e
            }
        }
    }

    private suspend fun pushDeletedTransactionAndResolveConflicts(remoteTransaction: RemoteTransaction) {
        try {
            remoteDataSource.deleteRemoteTransaction(remoteTransaction.orgSlug, remoteTransaction.id)
            Log.i("TransactionSync", "Successfully synced deleted transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            if (e.isNotFoundError()) {
                // Object already deleted on server - remove locally
                handleDeletedTransaction(remoteTransaction.id, "delete")
            } else {
                Log.e("TransactionSync", "Failed to sync deleted transaction ${remoteTransaction.id}", e)
                throw e
            }
        }
    }

    // --- Handle deleted transaction (404 scenario) ---
    private suspend fun handleDeletedTransaction(transactionId: String, operationType: String) {
        try {
            // Remove from local database
            localDataSource.deleteLocalTransactionById(transactionId)

            // Log the cleanup action
            Log.i("TransactionSync", "Transaction $transactionId was deleted on server during $operationType, removed locally")

            // You could also emit an event here for UI cleanup
            // eventBus.post(TransactionDeletedEvent(transactionId))

        } catch (e: Exception) {
            Log.e("TransactionSync", "Failed to clean up locally deleted transaction $transactionId", e)
            // Don't throw - we want to consider the sync successful since the object is gone
        }
    }

    override suspend fun push(operations: List<PendingOperation>) {
        for (operation in operations) {
            syncOperation(operation, operations)
        }
    }

    override suspend fun hasCachedData(): Boolean {
        return localDataSource.getLocalTransactionCount() != 0
    }

    private suspend fun syncOperation(currentOperation: PendingOperation, allOperations: List<PendingOperation>) {
        val transaction = currentOperation.parsePayload<RemoteTransaction>()

        // Count all pending operations for this entity
        val totalPending = allOperations.count { it.entityId == currentOperation.entityId }

        // Mark entity as syncing before pushing changes
        localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCING)

        try {
            when (currentOperation.operationType) {
                PendingOperationType.CREATE -> pushCreatedTransactionAndResolveConflicts(transaction)
                PendingOperationType.UPDATE -> pushUpdatedTransactionAndResolveConflicts(transaction)
                PendingOperationType.DELETE -> pushDeletedTransactionAndResolveConflicts(transaction)
                else -> {
                    Log.w("TransactionSync", "Unsupported operation type: ${currentOperation.operationType}")
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

            Log.i("TransactionSyncOperation", "Successfully processed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}")

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

                Log.e("TransactionSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)
            }
            // For 404 errors, we don't log as failures since they're handled gracefully
        }
    }

    override fun getEntity(): PendingOperationEntityType {
        return PendingOperationEntityType.Transaction
    }

    override suspend fun pullAll(organization: String) {
        Log.i("TransactionSync", "Full sync started")

        try {
            val remoteTransactions = remoteDataSource.getRemoteTransactions(organization)

            val domainTransactions = remoteTransactions.map { transactionMapper.mapRemoteToDomain(it) }
            val localTransactions = domainTransactions.map { transactionMapper.mapDomainToLocal(it) }

            localDataSource.saveLocalTransactions(localTransactions)
            Log.i("TransactionSync", "Full sync completed: ${localTransactions.size} transactions")
        } catch (e: Exception) {
            Log.e("TransactionSync", "Full sync failed: ${e.message}", e)
            throw e
        }
    }

    // --- Extension for 404 detection ---
    private fun Exception.isNotFoundError(): Boolean {
        return (this as? HttpException)?.code() == HTTP_NOT_FOUND
    }
}