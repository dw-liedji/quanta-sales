package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import kotlinx.serialization.json.Json

@Entity(
    tableName = "pending_operations",
    primaryKeys = ["entityType", "entityId", "orgId", "operationType"]
)
data class PendingOperation(
    val entityType: PendingOperationEntityType,
    val entityId: String,
    val orgSlug: String,
    val orgId: String,
    val operationType: PendingOperationType,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val failedAttempts: Int = 0
)
{
    // Helper function to create payload
    inline fun <reified T> parsePayload(): T {
        return Json.decodeFromString(payloadJson)
    }
}
