package com.datavite.eat.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "localBillingItems",
    foreignKeys = [ForeignKey(
        entity = LocalBilling::class,
        parentColumns = ["id"],
        childColumns = ["billingId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("billingId")]
)
data class LocalBillingItem(
    @PrimaryKey val id: String,
    val created: String,
    val modified: String,
    val orgSlug: String,
    val orgId: String,
    val orgUserId: String,
    val billingId: String,
    val stockId: String,
    val stockName: String,
    val quantity: Int,
    val unitPrice: Double,
    val syncStatus: SyncStatus
)

