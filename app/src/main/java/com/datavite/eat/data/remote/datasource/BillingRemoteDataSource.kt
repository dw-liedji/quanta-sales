package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteBilling

interface BillingRemoteDataSource {
    suspend fun getRemoteBillings(organization:String): List<RemoteBilling>
    suspend fun getRemoteBillingsChangesSince(organization: String, since: Long): List<RemoteBilling>
    suspend fun createRemoteBilling(organization:String, remoteBilling: RemoteBilling) : RemoteBilling
    suspend fun updateRemoteBilling(organization:String, remoteBilling: RemoteBilling) : RemoteBilling
    suspend fun deleteRemoteBilling(organization:String, remoteBillingId: String) : RemoteBilling
}