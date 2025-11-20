package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteCustomer
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.CustomerLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.CustomerMapper
import com.datavite.eat.data.remote.datasource.CustomerRemoteDataSource
import com.datavite.eat.data.sync.EntityType
import com.datavite.eat.data.sync.OperationType
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection.*
import javax.inject.Inject

class CustomerSyncServiceImpl @Inject constructor(
    private val remoteDataSource: CustomerRemoteDataSource,
    private val localDataSource: CustomerLocalDataSource,
    private val customerMapper: CustomerMapper,
    private val pendingOperationDao: PendingOperationDao,
) : CustomerSyncService {

    // --- Push CREATE ---
    private suspend fun pushCreatedCustomer(remoteCustomer: RemoteCustomer) {
        try {
            remoteDataSource.createRemoteCustomer(remoteCustomer.orgSlug, remoteCustomer)
            val updatedDomain = customerMapper.mapRemoteToDomain(remoteCustomer)
            val entities = customerMapper.mapDomainToLocal(updatedDomain)

            // Save parent + children
            localDataSource.insertLocalCustomer(entities)
            Log.i("CustomerSync", "Successfully synced created customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            handleCustomerSyncException(e, remoteCustomer.id, "CREATE")
            throw e
        }
    }

    // --- Push UPDATE ---
    private suspend fun pushUpdatedCustomer(remoteCustomer: RemoteCustomer) {
        try {
            remoteDataSource.updateRemoteCustomer(remoteCustomer.orgSlug, remoteCustomer)
            val updatedDomain = customerMapper.mapRemoteToDomain(remoteCustomer)
            val entities = customerMapper.mapDomainToLocal(updatedDomain)

            localDataSource.insertLocalCustomer(entities)
            Log.i("CustomerSync", "Successfully synced updated customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            handleCustomerSyncException(e, remoteCustomer.id, "UPDATE")
            throw e
        }
    }

    // --- Push DELETE ---
    private suspend fun pushDeletedCustomer(remoteCustomer: RemoteCustomer) {
        try {
            remoteDataSource.deleteRemoteCustomer(remoteCustomer.orgSlug, remoteCustomer.id)
            Log.i("CustomerSync", "Successfully synced deleted customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            handleCustomerSyncException(e, remoteCustomer.id, "DELETE")
            throw e
        }
    }

    // --- Comprehensive Exception Handling ---
    private suspend fun handleCustomerSyncException(e: Exception, customerId: String, operation: String) {
        when (e) {
            is HttpException -> {
                when (e.code()) {
                    HTTP_NOT_FOUND -> handleNotFound(customerId, operation)
                    HTTP_CONFLICT -> handleConflict(customerId, operation)
                    HTTP_UNAVAILABLE -> handleServiceUnavailable(customerId, operation)
                    HTTP_INTERNAL_ERROR -> handleServerError(customerId, operation)
                    HTTP_BAD_REQUEST -> handleBadRequest(customerId, operation)
                    HTTP_UNAUTHORIZED -> handleUnauthorized(customerId, operation)
                    HTTP_FORBIDDEN -> handleForbidden(customerId, operation)
                    HTTP_BAD_GATEWAY -> handleBadGateway(customerId, operation)
                    HTTP_GATEWAY_TIMEOUT -> handleGatewayTimeout(customerId, operation)
                    in 400..499 -> handleClientError(customerId, operation, e.code())
                    in 500..599 -> handleServerError(customerId, operation, e.code())
                    else -> handleGenericHttpError(customerId, operation, e.code())
                }
            }
            is IOException -> handleNetworkError(customerId, operation, e)
            else -> handleUnknownError(customerId, operation, e)
        }
    }

    // --- HTTP Status Code Handlers ---
    private suspend fun handleNotFound(customerId: String, operation: String) {
        Log.i("CustomerSync", "Customer $customerId not found during $operation - removing locally")
        // Object deleted on server - remove locally
        handleDeletedCustomer(customerId, operation)
    }

    private fun handleConflict(customerId: String, operation: String) {
        Log.w("CustomerSync", "Conflict detected for customer $customerId during $operation - requires resolution")
        // TODO: Implement conflict resolution logic
        // This could trigger a manual merge or use last-write-wins strategy
    }

    private fun handleServiceUnavailable(customerId: String, operation: String) {
        Log.w("CustomerSync", "Service unavailable for customer $customerId during $operation - retry later")
        // Server is down - will retry on next sync cycle
    }

    private fun handleServerError(customerId: String, operation: String, statusCode: Int? = null) {
        val codeInfo = if (statusCode != null) " (code: $statusCode)" else ""
        Log.e("CustomerSync", "Server error for customer $customerId during $operation$codeInfo")
        // Internal server error - retry with exponential backoff
    }

    private fun handleBadRequest(customerId: String, operation: String) {
        Log.e("CustomerSync", "Bad request for customer $customerId during $operation - check data format")
        // Invalid request - likely data format issue
    }

    private fun handleUnauthorized(customerId: String, operation: String) {
        Log.e("CustomerSync", "Unauthorized for customer $customerId during $operation - authentication required")
        // Token expired or invalid - trigger reauthentication
        // eventBus.post(AuthenticationRequiredEvent())
    }

    private fun handleForbidden(customerId: String, operation: String) {
        Log.e("CustomerSync", "Forbidden for customer $customerId during $operation - insufficient permissions")
        // User doesn't have permission - should not retry
    }

    private fun handleBadGateway(customerId: String, operation: String) {
        Log.w("CustomerSync", "Bad gateway for customer $customerId during $operation - retry later")
        // Proxy/gateway issue - retry with backoff
    }

    private fun handleGatewayTimeout(customerId: String, operation: String) {
        Log.w("CustomerSync", "Gateway timeout for customer $customerId during $operation - retry later")
        // Gateway timeout - retry with backoff
    }

    private fun handleClientError(customerId: String, operation: String, statusCode: Int) {
        Log.e("CustomerSync", "Client error $statusCode for customer $customerId during $operation")
        // Other 4xx errors - likely client-side issue
    }

    private fun handleGenericHttpError(customerId: String, operation: String, statusCode: Int) {
        Log.e("CustomerSync", "HTTP error $statusCode for customer $customerId during $operation")
        // Unhandled HTTP status code
    }

    // --- Network and Generic Error Handlers ---
    private fun handleNetworkError(customerId: String, operation: String, e: IOException) {
        Log.w("CustomerSync", "Network error for customer $customerId during $operation: ${e.message}")
        // Network connectivity issue - will retry when connection restored
    }

    private fun handleUnknownError(customerId: String, operation: String, e: Exception) {
        Log.e("CustomerSync", "Unknown error for customer $customerId during $operation: ${e.message}", e)
        // Unexpected error - log for debugging
    }

    // --- Handle deleted customer (404 scenario) ---
    private suspend fun handleDeletedCustomer(customerId: String, operationType: String) {
        try {
            // Remove from local database - FIXED: Changed from deleteLocalTransactionById to deleteLocalCustomerById
            localDataSource.deleteLocalCustomerById(customerId)

            // Log the cleanup action
            Log.i("CustomerSync", "Customer $customerId was deleted on server during $operationType, removed locally")

            // You could also emit an event here for UI cleanup
            // eventBus.post(CustomerDeletedEvent(customerId))

        } catch (e: Exception) {
            Log.e("CustomerSync", "Failed to clean up locally deleted customer $customerId", e)
            // Don't throw - we want to consider the sync successful since the object is gone
        }
    }

    // --- Push pending operations ---
    override suspend fun push(operations: List<PendingOperation>) {
        for (operation in operations) {
            syncOperation(operation, operations)
        }
    }

    override suspend fun hasCachedData(): Boolean =
        localDataSource.getLocalCustomerCount() != 0

    private suspend fun syncOperation(currentOperation: PendingOperation, allOperations: List<PendingOperation>) {
        val customer = currentOperation.parsePayload<RemoteCustomer>()
        val totalPending = allOperations.count { it.entityId == currentOperation.entityId }

        // Mark as syncing
        localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCING)

        try {
            when (currentOperation.operationType) {
                OperationType.CREATE -> pushCreatedCustomer(customer)
                OperationType.UPDATE -> pushUpdatedCustomer(customer)
                OperationType.DELETE -> pushDeletedCustomer(customer)
                else -> {} // ignore other types
            }

            // Remove operation after success
            pendingOperationDao.deleteByKeys(
                entityType = currentOperation.entityType,
                entityId = currentOperation.entityId,
                operationType = currentOperation.operationType,
                orgId = currentOperation.orgId
            )

            // Update sync status depending on remaining operations
            val newStatus = if ((totalPending - 1) > 0) SyncStatus.PENDING else SyncStatus.SYNCED
            localDataSource.updateSyncStatus(currentOperation.entityId, newStatus)

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
                val failureCount = pendingOperationDao.getFailureCount(
                    entityType = currentOperation.entityType,
                    entityId = currentOperation.entityId,
                    operationType = currentOperation.operationType,
                    orgId = currentOperation.orgId
                )
                val status = if (failureCount > 5) SyncStatus.FAILED else SyncStatus.PENDING
                localDataSource.updateSyncStatus(currentOperation.entityId, status)

                Log.e("CustomerSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)
            }
            // For 404 errors, we don't log as failures since they're handled gracefully
        }
    }

    override fun getEntity(): EntityType =
        EntityType.Customer

    // --- Full pull from remote ---
    override suspend fun pullAll(organization: String) {
        Log.i("CustomerSync", "Full customer sync started")
        try {
            val remoteCustomers = remoteDataSource.getRemoteCustomers(organization)
            val domainCustomers = remoteCustomers.map { customerMapper.mapRemoteToDomain(it) }
            val localEntities = domainCustomers.map { customerMapper.mapDomainToLocal(it) }

            localEntities.forEach {
                localDataSource.saveLocalCustomer(it)
            }

            Log.i("CustomerSync", "Full sync completed: ${localEntities.size} customers")
        } catch (e: Exception) {
            handleCustomerSyncException(e, "ALL", "PULL_ALL")
            throw e
        }
    }
}