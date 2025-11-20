package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteStock
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.StockLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.StockMapper
import com.datavite.eat.data.remote.datasource.StockRemoteDataSource
import com.datavite.eat.data.sync.EntityType
import com.datavite.eat.data.sync.OperationType
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection.*
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
            handleStockSyncException(e, remoteStock.id, "CREATE")
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
            handleStockSyncException(e, remoteStock.id, "UPDATE")
            throw e
        }
    }

    private suspend fun pushDeletedStockAndResolveConflicts(remoteStock: RemoteStock) {
        try {
            remoteDataSource.deleteRemoteStock(remoteStock.orgSlug, remoteStock.id)
            Log.i("StockSync", "Successfully synced deleted stock ${remoteStock.id}")
        } catch (e: Exception) {
            handleStockSyncException(e, remoteStock.id, "DELETE")
            throw e
        }
    }

    // --- Comprehensive Exception Handling ---
    private suspend fun handleStockSyncException(e: Exception, stockId: String, operation: String) {
        when (e) {
            is HttpException -> {
                when (e.code()) {
                    HTTP_NOT_FOUND -> handleNotFound(stockId, operation)
                    HTTP_CONFLICT -> handleConflict(stockId, operation)
                    HTTP_UNAVAILABLE -> handleServiceUnavailable(stockId, operation)
                    HTTP_INTERNAL_ERROR -> handleServerError(stockId, operation)
                    HTTP_BAD_REQUEST -> handleBadRequest(stockId, operation)
                    HTTP_UNAUTHORIZED -> handleUnauthorized(stockId, operation)
                    HTTP_FORBIDDEN -> handleForbidden(stockId, operation)
                    HTTP_BAD_GATEWAY -> handleBadGateway(stockId, operation)
                    HTTP_GATEWAY_TIMEOUT -> handleGatewayTimeout(stockId, operation)
                    in 400..499 -> handleClientError(stockId, operation, e.code())
                    in 500..599 -> handleServerError(stockId, operation, e.code())
                    else -> handleGenericHttpError(stockId, operation, e.code())
                }
            }
            is IOException -> handleNetworkError(stockId, operation, e)
            else -> handleUnknownError(stockId, operation, e)
        }
    }

    // --- HTTP Status Code Handlers ---
    private suspend fun handleNotFound(stockId: String, operation: String) {
        Log.i("StockSync", "Stock $stockId not found during $operation - removing locally")
        // Object deleted on server - remove locally
        handleDeletedStock(stockId, operation)
    }

    private fun handleConflict(stockId: String, operation: String) {
        Log.w("StockSync", "Conflict detected for stock $stockId during $operation - requires resolution")
        // TODO: Implement conflict resolution logic
        // This could trigger a manual merge or use last-write-wins strategy
    }

    private fun handleServiceUnavailable(stockId: String, operation: String) {
        Log.w("StockSync", "Service unavailable for stock $stockId during $operation - retry later")
        // Server is down - will retry on next sync cycle
    }

    private fun handleServerError(stockId: String, operation: String, statusCode: Int? = null) {
        val codeInfo = if (statusCode != null) " (code: $statusCode)" else ""
        Log.e("StockSync", "Server error for stock $stockId during $operation$codeInfo")
        // Internal server error - retry with exponential backoff
    }

    private fun handleBadRequest(stockId: String, operation: String) {
        Log.e("StockSync", "Bad request for stock $stockId during $operation - check data format")
        // Invalid request - likely data format issue
    }

    private fun handleUnauthorized(stockId: String, operation: String) {
        Log.e("StockSync", "Unauthorized for stock $stockId during $operation - authentication required")
        // Token expired or invalid - trigger reauthentication
        // eventBus.post(AuthenticationRequiredEvent())
    }

    private fun handleForbidden(stockId: String, operation: String) {
        Log.e("StockSync", "Forbidden for stock $stockId during $operation - insufficient permissions")
        // User doesn't have permission - should not retry
    }

    private fun handleBadGateway(stockId: String, operation: String) {
        Log.w("StockSync", "Bad gateway for stock $stockId during $operation - retry later")
        // Proxy/gateway issue - retry with backoff
    }

    private fun handleGatewayTimeout(stockId: String, operation: String) {
        Log.w("StockSync", "Gateway timeout for stock $stockId during $operation - retry later")
        // Gateway timeout - retry with backoff
    }

    private fun handleClientError(stockId: String, operation: String, statusCode: Int) {
        Log.e("StockSync", "Client error $statusCode for stock $stockId during $operation")
        // Other 4xx errors - likely client-side issue
    }

    private fun handleGenericHttpError(stockId: String, operation: String, statusCode: Int) {
        Log.e("StockSync", "HTTP error $statusCode for stock $stockId during $operation")
        // Unhandled HTTP status code
    }

    // --- Network and Generic Error Handlers ---
    private fun handleNetworkError(stockId: String, operation: String, e: IOException) {
        Log.w("StockSync", "Network error for stock $stockId during $operation: ${e.message}")
        // Network connectivity issue - will retry when connection restored
    }

    private fun handleUnknownError(stockId: String, operation: String, e: Exception) {
        Log.e("StockSync", "Unknown error for stock $stockId during $operation: ${e.message}", e)
        // Unexpected error - log for debugging
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
                OperationType.CREATE -> pushCreatedStockAndResolveConflicts(session)
                OperationType.UPDATE -> pushUpdatedStockAndResolveConflicts(session)
                OperationType.START_SESSION -> {}
                OperationType.END_SESSION -> {}
                OperationType.DELETE -> pushDeletedStockAndResolveConflicts(session)
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

                Log.e("StockSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)
            }
            // For 404 errors, we don't log as failures since they're handled gracefully
        }
    }

    override fun getEntity(): EntityType {
        return EntityType.Session
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
            handleStockSyncException(e, "ALL", "PULL_ALL")
            throw e
        }
    }
}