package com.datavite.eat.presentation.billing

import com.datavite.eat.domain.model.DomainBilling

data class BillingUiState(
    val availableBillings: List<DomainBilling> = emptyList(),
    val filteredBillings: List<DomainBilling> = emptyList(),
    val billingSearchQuery: String = "",
    val selectedBilling: DomainBilling? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isAddPaymentDialogVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false
)
