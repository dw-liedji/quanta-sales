package com.datavite.eat.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    @SerialName("withdrawal")
    WITHDRAWAL,
    @SerialName("deposit")
    DEPOSIT,
}
