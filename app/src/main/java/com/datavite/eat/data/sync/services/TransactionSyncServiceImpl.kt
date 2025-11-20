package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.TransactionLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.TransactionMapper
import com.datavite.eat.data.remote.datasource.TransactionRemoteDataSource
import com.datavite.eat.data.remote.model.RemoteTransaction
import com.datavite.eat.data.sync.EntityType
import com.datavite.eat.data.sync.OperationType
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection.*
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
            handleTransactionSyncException(e, remoteTransaction.id, "CREATE")
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
            handleTransactionSyncException(e, remoteTransaction.id, "UPDATE")
            throw e
        }
    }

    private suspend fun pushDeletedTransactionAndResolveConflicts(remoteTransaction: RemoteTransaction) {
        try {
            remoteDataSource.deleteRemoteTransaction(remoteTransaction.orgSlug, remoteTransaction.id)
            Log.i("TransactionSync", "Successfully synced deleted transaction ${remoteTransaction.id}")
        } catch (e: Exception) {
            handleTransactionSyncException(e, remoteTransaction.id, "DELETE")
            throw e
        }
    }

    // --- Comprehensive Exception Handling ---
    private suspend fun handleTransactionSyncException(e: Exception, transactionId: String, operation: String) {
        when (e) {
            is HttpException -> {
                when (e.code()) {
                    HTTP_NOT_FOUND -> handleNotFound(transactionId, operation)
                    HTTP_CONFLICT -> handleConflict(transactionId, operation)
                    HTTP_UNAVAILABLE -> handleServiceUnavailable(transactionId, operation)
                    HTTP_INTERNAL_ERROR -> handleServerError(transactionId, operation)
                    HTTP_BAD_REQUEST -> handleBadRequest(transactionId, operation)
                    HTTP_UNAUTHORIZED -> handleUnauthorized(transactionId, operation)
                    HTTP_FORBIDDEN -> handleForbidden(transactionId, operation)
                    HTTP_BAD_GATEWAY -> handleBadGateway(transactionId, operation)
                    HTTP_GATEWAY_TIMEOUT -> handleGatewayTimeout(transactionId, operation)
                    in 400..499 -> handleClientError(transactionId, operation, e.code())
                    in 500..599 -> handleServerError(transactionId, operation, e.code())
                    else -> handleGenericHttpError(transactionId, operation, e.code())
                }
            }
            is IOException -> handleNetworkError(transactionId, operation, e)
            else -> handleUnknownError(transactionId, operation, e)
        }
    }

    // --- HTTP Status Code Handlers ---
    private suspend fun handleNotFound(transactionId: String, operation: String) {
        Log.i("TransactionSync", "Transaction $transactionId not found during $operation - removing locally")
        // Object deleted on server - remove locally
        handleDeletedTransaction(transactionId, operation)
    }

    private fun handleConflict(transactionId: String, operation: String) {
        Log.w("TransactionSync", "Conflict detected for transaction $transactionId during $operation - requires resolution")
        // TODO: Implement conflict resolution logic
        // This could trigger a manual merge or use last-write-wins strategy
    }

    private fun handleServiceUnavailable(transactionId: String, operation: String) {
        Log.w("TransactionSync", "Service unavailable for transaction $transactionId during $operation - retry later")
        // Server is down - will retry on next sync cycle
    }

    private fun handleServerError(transactionId: String, operation: String, statusCode: Int? = null) {
        val codeInfo = if (statusCode != null) " (code: $statusCode)" else ""
        Log.e("TransactionSync", "Server error for transaction $transactionId during $operation$codeInfo")
        // Internal server error - retry with exponential backoff
    }

    private fun handleBadRequest(transactionId: String, operation: String) {
        Log.e("TransactionSync", "Bad request for transaction $transactionId during $operation - check data format")
        // Invalid request - likely data format issue
    }

    private fun handleUnauthorized(transactionId: String, operation: String) {
        Log.e("TransactionSync", "Unauthorized for transaction $transactionId during $operation - authentication required")
        // Token expired or invalid - trigger reauthentication
        // eventBus.post(AuthenticationRequiredEvent())
    }

    private fun handleForbidden(transactionId: String, operation: String) {
        Log.e("TransactionSync", "Forbidden for transaction $transactionId during $operation - insufficient permissions")
        // User doesn't have permission - should not retry
    }

    private fun handleBadGateway(transactionId: String, operation: String) {
        Log.w("TransactionSync", "Bad gateway for transaction $transactionId during $operation - retry later")
        // Proxy/gateway issue - retry with backoff
    }

    private fun handleGatewayTimeout(transactionId: String, operation: String) {
        Log.w("TransactionSync", "Gateway timeout for transaction $transactionId during $operation - retry later")
        // Gateway timeout - retry with backoff
    }

    private fun handleClientError(transactionId: String, operation: String, statusCode: Int) {
        Log.e("TransactionSync", "Client error $statusCode for transaction $transactionId during $operation")
        // Other 4xx errors - likely client-side issue
    }

    private fun handleGenericHttpError(transactionId: String, operation: String, statusCode: Int) {
        Log.e("TransactionSync", "HTTP error $statusCode for transaction $transactionId during $operation")
        // Unhandled HTTP status code
    }

    // --- Network and Generic Error Handlers ---
    private fun handleNetworkError(transactionId: String, operation: String, e: IOException) {
        Log.w("TransactionSync", "Network error for transaction $transactionId during $operation: ${e.message}")
        // Network connectivity issue - will retry when connection restored
    }

    private fun handleUnknownError(transactionId: String, operation: String, e: Exception) {
        Log.e("TransactionSync", "Unknown error for transaction $transactionId during $operation: ${e.message}", e)
        // Unexpected error - log for debugging
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
                OperationType.CREATE -> pushCreatedTransactionAndResolveConflicts(transaction)
                OperationType.UPDATE -> pushUpdatedTransactionAndResolveConflicts(transaction)
                OperationType.DELETE -> pushDeletedTransactionAndResolveConflicts(transaction)
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
            val isNotFoundError = e is HttpException && e.code() == HTTP_NOT_FOUND
            if (!isNotFoundError) {
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

    override fun getEntity(): EntityType {
        return EntityType.Transaction
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
            handleTransactionSyncException(e, "ALL", "PULL_ALL")
            throw e
        }
    }
}