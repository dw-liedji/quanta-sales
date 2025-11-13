package com.datavite.eat.data.local.datasource

import FilterOption
import com.datavite.eat.data.local.model.LocalStock
import com.datavite.eat.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface StockLocalDataSource {
    suspend fun getLocalStocksFlow(): Flow<List<LocalStock>>
    suspend fun getLocalStocksFor(searchQuery:String, filterOption: FilterOption): List<LocalStock>
    suspend fun getLocalStocksForFilterOption(filterOption: FilterOption): List<LocalStock>
    suspend fun getLocalStockById(localStockId:String): LocalStock?
    suspend fun insertLocalStock(localStock: LocalStock)
    suspend fun saveLocalStocks(localStocks:List<LocalStock>)
    suspend fun updateSyncStatus(id: String,  syncStatus: SyncStatus)
    suspend fun clear()
    suspend fun deleteLocalStock(localStock: LocalStock)
    suspend fun getLocalStockCount():Int

}