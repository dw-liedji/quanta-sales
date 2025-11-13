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
            Log.e("TransactionSync", "Success to sync created transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            e.printStackTrace()
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
            Log.e("TransactionSync", "Success to sync updated transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TransactionSync", "Failed to sync updated transaction ${remoteTransaction.id}", e)
            throw e
        }
    }

    private suspend fun pushDeletedTransactionAndResolveConflicts(remoteTransaction: RemoteTransaction) {
        try {
            remoteDataSource.deleteRemoteTransaction(remoteTransaction.orgSlug, remoteTransaction.id)
            Log.e("TransactionSync", "Success to sync deleted transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TransactionSync", "Failed to sync deleted transaction ${remoteTransaction.id}", e)
            throw e
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
            pendingOperationDao.deleteById(currentOperation.id)

            // If other pending operations remain, status is still pending
            if ((totalPending - 1) > 0) {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.PENDING)
            } else {
                localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCED)
            }
            Log.i("TransactionSyncOperation", "Success to sync transaction operation ${currentOperation.id}")

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
            Log.e("TransactionSyncOperation", "Failed to sync transaction operation ${currentOperation.id}")

            e.printStackTrace()
        }
    }

    override fun getEntity(): PendingOperationEntityType {
        return PendingOperationEntityType.Transaction
    }

    override suspend fun pullAll(organization: String) {
        Log.e("TransactionSync", "Full sync started")

        try {
            val remoteTransactions = remoteDataSource.getRemoteTransactions(organization)

            val domainTransactions = remoteTransactions.map { transactionMapper.mapRemoteToDomain(it) }
            val localTransactions = domainTransactions.map { transactionMapper.mapDomainToLocal(it) }
            //localDataSource.clear()
            localDataSource.saveLocalTransactions(localTransactions)
            Log.e("TransactionSync", "Full sync success ${localTransactions.size} transactions")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TransactionSync", "Full sync failed: ${e.message}", e)
            throw e
        }
    }
}