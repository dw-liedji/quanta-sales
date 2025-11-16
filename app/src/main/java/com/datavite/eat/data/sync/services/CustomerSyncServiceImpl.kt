package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteCustomer
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.CustomerLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.CustomerMapper
import com.datavite.eat.data.remote.datasource.CustomerRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
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
            localDataSource.insertLocalCustomer(
                entities
            )
            Log.i("CustomerSync", "Successfully synced created customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            Log.e("CustomerSync", "Failed to sync created customer ${remoteCustomer.id}", e)
            throw e
        }
    }

    // --- Push UPDATE ---
    private suspend fun pushUpdatedCustomer(remoteCustomer: RemoteCustomer) {
        try {
            remoteDataSource.updateRemoteCustomer(remoteCustomer.orgSlug, remoteCustomer)
            val updatedDomain = customerMapper.mapRemoteToDomain(remoteCustomer)
            val entities = customerMapper.mapDomainToLocal(updatedDomain)

            localDataSource.insertLocalCustomer(
                entities
            )
            Log.i("CustomerSync", "Successfully synced updated customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            Log.e("CustomerSync", "Failed to sync updated customer ${remoteCustomer.id}", e)
            throw e
        }
    }

    // --- Push DELETE ---
    private suspend fun pushDeletedCustomer(remoteCustomer: RemoteCustomer) {
        try {
            remoteDataSource.deleteRemoteCustomer(remoteCustomer.orgSlug, remoteCustomer.id)
            Log.i("CustomerSync", "Successfully synced deleted customer ${remoteCustomer.id}")
        } catch (e: Exception) {
            Log.e("CustomerSync", "Failed to sync deleted customer ${remoteCustomer.id}", e)
            throw e
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
                PendingOperationType.CREATE -> pushCreatedCustomer(customer)
                PendingOperationType.UPDATE -> pushUpdatedCustomer(customer)
                PendingOperationType.DELETE -> pushDeletedCustomer(customer)
                else -> {} // ignore other types
            }

            // Remove operation after success
            pendingOperationDao.deleteByKeys(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)

            // Update sync status depending on remaining operations
            val newStatus = if ((totalPending - 1) > 0) SyncStatus.PENDING else SyncStatus.SYNCED
            localDataSource.updateSyncStatus(currentOperation.entityId, newStatus)

        } catch (e: Exception) {
            pendingOperationDao.incrementFailureCount(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)
            val failureCount = pendingOperationDao.getFailureCount(entityType = currentOperation.entityType, entityId = currentOperation.entityId, operationType = currentOperation.operationType, orgId = currentOperation.orgId)
            val status = if (failureCount > 5) SyncStatus.FAILED else SyncStatus.PENDING
            localDataSource.updateSyncStatus(currentOperation.entityId, status)

            Log.e("BillingSyncOperation", "Failed operation ${currentOperation.operationType} ${currentOperation.entityType} ${currentOperation.entityId}", e)
        }
    }

    override fun getEntity(): PendingOperationEntityType =
        PendingOperationEntityType.Customer

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
            Log.e("CustomerSync", "Full sync failed", e)
            throw e
        }
    }

    // Extension to convert mapper entities to Room relation
}
