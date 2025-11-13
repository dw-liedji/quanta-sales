package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteStock
import com.datavite.eat.data.remote.service.RemoteStockService
import javax.inject.Inject

class StockRemoteDataSourceImpl @Inject constructor(
    private val remoteStockService: RemoteStockService
) : StockRemoteDataSource {
    override suspend fun getRemoteStocks(organization:String): List<RemoteStock> {
        return remoteStockService.getRemoteStocks(organization)
    }

    override suspend fun createRemoteStock(organization:String, remoteStock: RemoteStock): RemoteStock {
        return remoteStockService.createRemoteStock(organization, remoteStock)
    }

    override suspend fun updateRemoteStock(organization:String, remoteStock: RemoteStock): RemoteStock {
        return remoteStockService.updateRemoteStock(organization, remoteStock.id, remoteStock)
    }

  override suspend fun deleteRemoteStock(organization:String, remoteStockId: String): RemoteStock {
        return remoteStockService.deleteRemoteStock(organization, remoteStockId)
    }
}