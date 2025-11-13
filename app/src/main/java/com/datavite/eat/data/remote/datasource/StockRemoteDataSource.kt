package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteStock

interface StockRemoteDataSource {
    suspend fun getRemoteStocks(organization:String): List<RemoteStock>
    suspend fun createRemoteStock(organization:String, remoteStock: RemoteStock) : RemoteStock
    suspend fun updateRemoteStock(organization:String, remoteStock: RemoteStock) : RemoteStock
    suspend fun deleteRemoteStock(organization:String, remoteStockId: String) : RemoteStock
}