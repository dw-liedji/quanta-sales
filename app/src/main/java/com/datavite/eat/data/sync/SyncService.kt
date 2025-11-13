package com.datavite.eat.data.sync

import com.datavite.eat.data.local.model.PendingOperation
import com.datavite.eat.domain.PendingOperationEntityType

interface SyncService {
    suspend fun pullAll(organization: String)
    suspend fun push(operations: List<PendingOperation>)
    suspend fun hasCachedData(): Boolean

    fun getEntity() : PendingOperationEntityType
}