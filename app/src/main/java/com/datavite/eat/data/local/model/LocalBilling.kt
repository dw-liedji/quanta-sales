package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName


@Entity(tableName = "localBillings")
data class LocalBilling(
    @PrimaryKey val id: String,
    val created: String,
    val modified: String,
    val orgSlug: String,
    val orgId: String,
    val orgUserId: String,
    val orgUserName: String,
    val billNumber: String,
    val placedAt: String,
    val customerId: String,
    val customerName: String,
    val customerPhoneNumber: String,
    val isPay: Boolean,
    val isApproved: Boolean = false,
    val isDelivered: Boolean = false,
    val syncStatus: SyncStatus
)



