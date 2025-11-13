package com.datavite.eat.data.repository

import FilterOption
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.datavite.eat.data.local.dao.PendingNotificationDao
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.CustomerLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.CustomerMapper
import com.datavite.eat.data.remote.datasource.CustomerRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import com.datavite.eat.domain.model.DomainCustomer
import com.datavite.eat.domain.notification.NotificationBus
import com.datavite.eat.domain.notification.NotificationEvent
import com.datavite.eat.domain.repository.CustomerRepository
import com.datavite.eat.utils.JsonConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val localDataSource: CustomerLocalDataSource,
    private val remoteDataSource: CustomerRemoteDataSource,
    private val customerMapper: CustomerMapper,
    private val pendingOperationDao: PendingOperationDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val notificationBus: NotificationBus
) : CustomerRepository {

    override suspend fun getDomainCustomersFlow(): Flow<List<DomainCustomer>> {
        return localDataSource.getLocalCustomersFlow().map { domainCustomers -> domainCustomers.map { customerMapper.mapLocalToDomain(it) } }
    }


    override suspend fun getDomainCustomerById(domainCustomerId: String): DomainCustomer? {
        val domainCustomer = localDataSource.getLocalCustomerById(domainCustomerId)
        return domainCustomer?.let { customerMapper.mapLocalToDomain(domainCustomer) }
    }

    override suspend fun createCustomer(domainCustomer: DomainCustomer) {

        val pendingDomainCustomer = domainCustomer.copy(
            created = LocalDateTime.now().toString(),
            modified = LocalDateTime.now().toString(),
        )

        val local = customerMapper.mapDomainToLocal(pendingDomainCustomer)
        val remote = customerMapper.mapDomainToRemote(pendingDomainCustomer)

        val operation = PendingOperation(
            orgSlug = domainCustomer.orgSlug,
            entityId = domainCustomer.id,
            entityType = PendingOperationEntityType.Customer,
            operationType = PendingOperationType.CREATE,
            payloadJson = JsonConverter.toJson(remote),
        )

        try {
            localDataSource.insertLocalCustomer(local)
            pendingOperationDao.insert(operation)
            notificationBus.emit(NotificationEvent.Success("Customer created successfully"))
        }catch (e: SQLiteConstraintException) {
            notificationBus.emit(NotificationEvent.Failure("Another domainCustomer with the same period already exists"))
        }

    }

    override suspend fun saveCustomer(domainCustomer: DomainCustomer) {
        val pendingDomainCustomer = domainCustomer.copy(
            created = LocalDateTime.now().toString(),
            modified = LocalDateTime.now().toString(),
        )

        val local = customerMapper.mapDomainToLocal(pendingDomainCustomer)
        localDataSource.saveLocalCustomer(local)
    }

    private suspend fun updateCustomer(domainCustomer: DomainCustomer) {
        val pendingDomainCustomer = domainCustomer.copy(syncStatus = SyncStatus.PENDING)
        val local = customerMapper.mapDomainToLocal(pendingDomainCustomer)
        val remote = customerMapper.mapDomainToRemote(pendingDomainCustomer)

        val operation = PendingOperation(
            orgSlug = domainCustomer.orgSlug,
            entityId = domainCustomer.id,
            entityType = PendingOperationEntityType.Customer,
            operationType = PendingOperationType.UPDATE,
            payloadJson = JsonConverter.toJson(remote),
        )

        localDataSource.insertLocalCustomer(local)
        pendingOperationDao.insert(operation)
    }

    override suspend fun deleteCustomer(domainCustomer: DomainCustomer) {

        val pendingDomainCustomer = domainCustomer.copy(syncStatus = SyncStatus.PENDING)
        val local = customerMapper.mapDomainToLocal(pendingDomainCustomer)
        val remote = customerMapper.mapDomainToRemote(pendingDomainCustomer)

        val operation = PendingOperation(
            orgSlug = domainCustomer.orgSlug,
            entityId = domainCustomer.id,
            entityType = PendingOperationEntityType.Customer,
            operationType = PendingOperationType.DELETE,
            payloadJson = JsonConverter.toJson(remote),
        )

        localDataSource.deleteLocalCustomer(local)
        pendingOperationDao.insert(operation)

    }

    override suspend fun fetchIfEmpty(organization: String) {
        try {
            if (localDataSource.getLocalCustomerCount() == 0){
                val remoteCustomers = remoteDataSource.getRemoteCustomers(organization)
                val domainCustomers = remoteCustomers.map { customerMapper.mapRemoteToDomain(it) }
                val localCustomers = domainCustomers.map { customerMapper.mapDomainToLocal(it) }
                localDataSource.clear()
                localDataSource.saveLocalCustomers(localCustomers)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CustomerRepository", " error ${e.message}")
        }
    }

    override suspend fun getDomainCustomersFor(
        searchQuery: String,
        filterOption: FilterOption
    ): List<DomainCustomer> {
        TODO("Not yet implemented")
    }

    override suspend fun getDomainCustomersForFilterOption(filterOption: FilterOption): List<DomainCustomer> {
        TODO("Not yet implemented")
    }
}
