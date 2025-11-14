package com.datavite.eat.domain.model

import com.datavite.eat.data.local.model.SyncStatus

data class DomainCustomer(
    val id: String,
    val created: String,
    val modified: String,
    val orgSlug: String,
    val orgId: String,
    val name: String,
    val phoneNumber: String?,
    val syncStatus: SyncStatus,
    )
