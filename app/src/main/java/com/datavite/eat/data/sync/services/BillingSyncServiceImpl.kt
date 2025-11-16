package com.datavite.eat.data.sync.services

import android.util.Log
import com.datavite.eat.data.remote.model.RemoteBilling
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.BillingLocalDataSource
import com.datavite.eat.data.local.model.LocalBillingWithItemsAndPaymentsRelation
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.BillingMapper
import com.datavite.eat.data.remote.datasource.BillingRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import javax.inject.Inject

class BillingSyncServiceImpl @Inject constructor(
    private val remoteDataSource: BillingRemoteDataSource,
    private val localDataSource: BillingLocalDataSource,
    private val billingMapper: BillingMapper,
    private val pendingOperationDao: PendingOperationDao,
) : BillingSyncService {

    // --- Push CREATE ---
    private suspend fun pushCreatedBilling(remoteBilling: RemoteBilling) {
        try {
            remoteDataSource.createRemoteBilling(remoteBilling.orgSlug, remoteBilling)
            val updatedDomain = billingMapper.mapRemoteToDomain(remoteBilling)
            val entities = billingMapper.mapDomainToLocalBillingWithItemsAndPaymentsRelation(updatedDomain)

            // Save parent + children
            localDataSource.insertLocalBillingWithItemsAndPaymentsRelation(
                entities
            )
            Log.i("BillingSync", "Successfully synced created billing ${remoteBilling.id}")
        } catch (e: Exception) {
            Log.e("BillingSync", "Failed to sync created billing ${remoteBilling.id}", e)
            throw e
        }
    }

    // --- Push UPDATE ---
    private suspend fun pushUpdatedBilling(remoteBilling: RemoteBilling) {
        try {
            remoteDataSource.updateRemoteBilling(remoteBilling.orgSlug, remoteBilling)
            val updatedDomain = billingMapper.mapRemoteToDomain(remoteBilling)
            val entities = billingMapper.mapDomainToLocalBillingWithItemsAndPaymentsRelation(updatedDomain)

            localDataSource.insertLocalBillingWithItemsAndPaymentsRelation(
                entities
            )
            Log.i("BillingSync", "Successfully synced updated billing ${remoteBilling.id}")
        } catch (e: Exception) {
            Log.e("BillingSync", "Failed to sync updated billing ${remoteBilling.id}", e)
            throw e
        }
    }

    // --- Push DELETE ---
    private suspend fun pushDeletedBilling(remoteBilling: RemoteBilling) {
        try {
            remoteDataSource.deleteRemoteBilling(remoteBilling.orgSlug, remoteBilling.id)
            Log.i("BillingSync", "Successfully synced deleted billing ${remoteBilling.id}")
        } catch (e: Exception) {
            Log.e("BillingSync", "Failed to sync deleted billing ${remoteBilling.id}", e)
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
        localDataSource.getLocalBillingCount() != 0

    private suspend fun syncOperation(currentOperation: PendingOperation, allOperations: List<PendingOperation>) {
        val billing = currentOperation.parsePayload<RemoteBilling>()
        val totalPending = allOperations.count { it.entityId == currentOperation.entityId }

        // Mark as syncing
        localDataSource.updateSyncStatus(currentOperation.entityId, SyncStatus.SYNCING)

        try {
            when (currentOperation.operationType) {
                PendingOperationType.CREATE -> pushCreatedBilling(billing)
                PendingOperationType.UPDATE -> pushUpdatedBilling(billing)
                PendingOperationType.DELETE -> pushDeletedBilling(billing)
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
        PendingOperationEntityType.Billing

    // --- Full pull from remote ---
    override suspend fun pullAll(organization: String) {
        Log.i("BillingSync", "Full billing sync started")
        try {
            val remoteBillings = remoteDataSource.getRemoteBillings(organization)
            val domainBillings = remoteBillings.map { billingMapper.mapRemoteToDomain(it) }
            val localEntities = domainBillings.map { billingMapper.mapDomainToLocalBillingWithItemsAndPaymentsRelation(it) }

            localEntities.forEach {
                localDataSource.insertLocalBillingWithItemsAndPaymentsRelation(it)
            }

            Log.i("BillingSync", "Full sync completed: ${localEntities.size} billings")
        } catch (e: Exception) {
            Log.e("BillingSync", "Full sync failed", e)
            throw e
        }
    }

    // Extension to convert mapper entities to Room relation
}
