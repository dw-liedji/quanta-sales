package com.datavite.eat.domain.repository

import FilterOption
import com.datavite.eat.domain.model.DomainTransaction
import com.datavite.eat.utils.TransactionBroker
import com.datavite.eat.utils.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getDomainTransactionsFlow(): Flow<List<DomainTransaction>>
    suspend fun getDomainTransactionsFor(searchQuery: String, filterOption: FilterOption): List<DomainTransaction>
    suspend fun getDomainTransactionsForFilterOption(filterOption: FilterOption): List<DomainTransaction>
    suspend fun getDomainTransactionById(domainTransactionId: String): DomainTransaction?
    suspend fun createTransaction(domainTransaction: DomainTransaction)
    suspend fun deleteTransaction(domainTransaction: DomainTransaction)
    suspend fun fetchIfEmpty(organization: String)

    // Additional transaction-specific methods
    suspend fun getDomainTransactionsByType(transactionType: TransactionType): Flow<List<DomainTransaction>>
    suspend fun getDomainTransactionsByBroker(transactionBroker: TransactionBroker): Flow<List<DomainTransaction>>
    suspend fun getDomainTransactionsByUser(orgUserId: String): Flow<List<DomainTransaction>>
    suspend fun getDomainTransactionsByDateRange(startDate: String, endDate: String): Flow<List<DomainTransaction>>
}