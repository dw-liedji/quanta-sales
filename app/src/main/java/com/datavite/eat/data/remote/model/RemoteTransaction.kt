package com.datavite.eat.data.remote.model

import androidx.room.PrimaryKey
import com.datavite.eat.utils.TransactionBroker
import com.datavite.eat.utils.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteTransaction(
    @PrimaryKey val id: String,
    @SerialName("created") val created: String,
    @SerialName("modified") val modified: String,
    @SerialName("organization_slug") val orgSlug: String,
    @SerialName("organization_id") val orgId: String,
    @SerialName("organization_user_id") val orgUserId: String,
    @SerialName("participant")  val participant: String,
    @SerialName("reason") val reason: String,
    @SerialName("amount") val amount: Double,
    @SerialName("transaction_type")val transactionType: TransactionType,
    @SerialName("transaction_broker") val transactionBroker: TransactionBroker,
)
