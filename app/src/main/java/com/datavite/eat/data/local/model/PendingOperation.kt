package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType
import kotlinx.serialization.json.Json

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: PendingOperationEntityType,         // "session", "attendance", etc.
    val entityId: String,           // UUID or numeric ID
    val orgSlug: String,           // UUID or numeric ID
    val operationType: PendingOperationType,      // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,        // Full object JSON
    val createdAt: Long = System.currentTimeMillis(),  // For FIFO order
    val failedAttempts: Int = 0     // For retry limit (optional safety)
){
    // Helper function to create payload
    inline fun <reified T> parsePayload(): T {
        return Json.decodeFromString(payloadJson)
    }
}
