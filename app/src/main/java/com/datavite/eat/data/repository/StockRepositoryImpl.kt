package com.datavite.eat.data.repository

import FilterOption
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.datavite.eat.data.local.dao.PendingNotificationDao
import com.datavite.eat.data.local.dao.PendingOperationDao
import com.datavite.eat.data.local.datasource.StockLocalDataSource
import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.data.local.model.SyncStatus
import com.datavite.eat.data.mapper.StockMapper
import com.datavite.eat.data.remote.datasource.StockRemoteDataSource
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import com.datavite.eat.domain.model.DomainStock
import com.datavite.eat.domain.notification.NotificationBus
import com.datavite.eat.domain.notification.NotificationEvent
import com.datavite.eat.domain.repository.StockRepository
import com.datavite.eat.utils.JsonConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class StockRepositoryImpl @Inject constructor(
    private val localDataSource: StockLocalDataSource,
    private val remoteDataSource: StockRemoteDataSource,
    private val stockMapper: StockMapper,
    private val pendingOperationDao: PendingOperationDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val notificationBus: NotificationBus
) : StockRepository {

    override suspend fun getDomainStocksFlow(): Flow<List<DomainStock>> {
        return localDataSource.getLocalStocksFlow().map { domainStocks -> domainStocks.map { stockMapper.mapLocalToDomain(it) } }
    }


    override suspend fun getDomainStockById(domainStockId: String): DomainStock? {
        val domainStock = localDataSource.getLocalStockById(domainStockId)
        return domainStock?.let { stockMapper.mapLocalToDomain(domainStock) }
    }

    override suspend fun createStock(domainStock: DomainStock) {

        val pendingDomainStock = domainStock.copy(
            created = LocalDateTime.now().toString(),
            modified = LocalDateTime.now().toString(),
        )

        val local = stockMapper.mapDomainToLocal(pendingDomainStock)
        val remote = stockMapper.mapDomainToRemote(pendingDomainStock)

        val operation = PendingOperation(
            orgSlug = domainStock.orgSlug,
            orgId = domainStock.orgId,
            entityId = domainStock.id,
            entityType = PendingOperationEntityType.Stock,
            operationType = PendingOperationType.CREATE,
            payloadJson = JsonConverter.toJson(remote),
        )

        try {
            localDataSource.insertLocalStock(local)
            pendingOperationDao.insert(operation)
            notificationBus.emit(NotificationEvent.Success("Stock created successfully"))
        }catch (e: SQLiteConstraintException) {
            notificationBus.emit(NotificationEvent.Failure("Another domainStock with the same period already exists"))
        }

    }

    private suspend fun updateStock(domainStock: DomainStock) {
        val pendingDomainStock = domainStock.copy(syncStatus = SyncStatus.PENDING)
        val local = stockMapper.mapDomainToLocal(pendingDomainStock)
        val remote = stockMapper.mapDomainToRemote(pendingDomainStock)

        val operation = PendingOperation(
            orgSlug = domainStock.orgSlug,
            orgId = domainStock.orgId,
            entityId = domainStock.id,
            entityType = PendingOperationEntityType.Stock,
            operationType = PendingOperationType.UPDATE,
            payloadJson = JsonConverter.toJson(remote),
        )

        localDataSource.insertLocalStock(local)
        pendingOperationDao.insert(operation)
    }

    override suspend fun deleteStock(domainStock: DomainStock) {

        val pendingDomainStock = domainStock.copy(syncStatus = SyncStatus.PENDING)
        val local = stockMapper.mapDomainToLocal(pendingDomainStock)
        val remote = stockMapper.mapDomainToRemote(pendingDomainStock)

        val operation = PendingOperation(
            orgSlug = domainStock.orgSlug,
            orgId = domainStock.orgId,
            entityId = domainStock.id,
            entityType = PendingOperationEntityType.Stock,
            operationType = PendingOperationType.DELETE,
            payloadJson = JsonConverter.toJson(remote),
        )

        localDataSource.deleteLocalStock(local)
        pendingOperationDao.insert(operation)

    }

    override suspend fun fetchIfEmpty(organization: String) {
        try {
            if (localDataSource.getLocalStockCount() == 0){
                val remoteStocks = remoteDataSource.getRemoteStocks(organization)
                val domainStocks = remoteStocks.map { stockMapper.mapRemoteToDomain(it) }
                val localStocks = domainStocks.map { stockMapper.mapDomainToLocal(it) }
                localDataSource.clear()
                localDataSource.saveLocalStocks(localStocks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("StockRepository", " error ${e.message}")
        }
    }

    override suspend fun getDomainStocksFor(
        searchQuery: String,
        filterOption: FilterOption
    ): List<DomainStock> {
        TODO("Not yet implemented")
    }

    override suspend fun getDomainStocksForFilterOption(filterOption: FilterOption): List<DomainStock> {
        TODO("Not yet implemented")
    }
}
