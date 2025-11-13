package com.datavite.eat.domain.model

import com.datavite.eat.R
import com.datavite.eat.data.local.model.SyncStatus

data class DomainStock(
    val id: String,
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
    val imageUrl: Int = R.drawable.no_org,
)
