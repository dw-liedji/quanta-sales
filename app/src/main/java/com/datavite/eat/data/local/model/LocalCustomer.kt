package com.datavite.eat.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "localCustomers", indices = [
    Index(
        value = ["orgId", "phoneNumber"],
        unique = true
    )
])
data class LocalCustomer(
    @PrimaryKey val id: String,
    val created: String,
    val modified: String,
    val orgSlug: String,
    val orgId: String,
    val name: String,
    val phoneNumber: String,
    val syncStatus: SyncStatus,
    )