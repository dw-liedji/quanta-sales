package com.datavite.eat.presentation.shopping

import com.datavite.eat.domain.model.DomainStock

data class SelectedDomainStock(
    val domainStock: DomainStock,
    var quantity: Int = 1,
    var price: Double = domainStock.billingPrice,
    var isPriceLocked: Boolean = true
) {
    val subtotal: Double get() = quantity * price
}
