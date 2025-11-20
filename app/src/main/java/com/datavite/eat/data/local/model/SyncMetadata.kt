package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.datavite.eat.data.sync.EntityType

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey
    val entityType: EntityType,
    val lastSyncTimestamp: Long,
    val lastSyncSuccess: Boolean,
    val syncVersion: Int = 1,
    val lastError: String? = null,
    val retryCount: Int = 0
)
