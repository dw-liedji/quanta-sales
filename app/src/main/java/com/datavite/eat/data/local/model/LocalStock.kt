package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "localStocks")
data class LocalStock(
    @PrimaryKey val id: String,
    val itemId: String,
    val created: String,
    val modified: String,
    val orgSlug: String,
    val itemName: String,
    val isActive: Boolean,
    val orgId: String,
    val categoryId: String,
    val categoryName: String,
    val batchNumber: String,
    val receivedDate: String,
    val expirationDate: String,
    val purchasePrice: Double,
    val billingPrice: Double,
    val quantity: Int,
    val syncStatus: SyncStatus,
    )
