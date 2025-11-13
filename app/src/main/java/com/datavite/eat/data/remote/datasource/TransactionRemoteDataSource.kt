package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteTransaction

interface TransactionRemoteDataSource {
    suspend fun getRemoteTransactions(organization: String): List<RemoteTransaction>
    suspend fun createRemoteTransaction(organization: String, remoteTransaction: RemoteTransaction): RemoteTransaction
    suspend fun updateRemoteTransaction(organization: String, remoteTransaction: RemoteTransaction): RemoteTransaction
    suspend fun deleteRemoteTransaction(organization: String, transactionId: String): RemoteTransaction
}