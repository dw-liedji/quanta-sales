package com.datavite.eat.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteStock(
    @SerialName("id") val id: String,
    @SerialName("created")  val created: String,
    @SerialName("modified")  val modified: String,
    @SerialName("organization_slug") val orgSlug: String,
    @SerialName("item") val itemId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("organization_id") val orgId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("batch_number") val batchNumber: String,
    @SerialName("received_date") val receivedDate: String,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("facturation_price") val billingPrice: Double,
    @SerialName("quantity") val quantity: Int,
)
