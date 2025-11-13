package com.datavite.eat.presentation.transaction

import com.datavite.eat.domain.model.DomainTransaction
import com.datavite.eat.utils.TransactionBroker
import com.datavite.eat.utils.TransactionType

data class TransactionUiState(
    val transactions: List<DomainTransaction> = emptyList(),
    val filteredTransactions: List<DomainTransaction> = emptyList(),
    val selectedTransaction: DomainTransaction? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,

    // Filter states
    val searchQuery: String = "",
    val selectedTransactionType: TransactionType? = null,
    val selectedBroker: TransactionBroker? = null,
    val dateRange: ClosedRange<Long>? = null,

    // New transaction form
    val isCreatingTransaction: Boolean = false,
    val transactionAmount: String = "",
    val transactionReason: String = "",
    val selectedType: TransactionType = TransactionType.DEPOSIT,
    val selectedTransactionBroker: TransactionBroker = TransactionBroker.CASHIER,
    val participant: String = ""
)