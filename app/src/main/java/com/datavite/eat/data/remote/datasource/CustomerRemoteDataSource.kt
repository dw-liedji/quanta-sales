package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteCustomer

interface CustomerRemoteDataSource {
    suspend fun getRemoteCustomers(organization:String): List<RemoteCustomer>
    suspend fun createRemoteCustomer(organization:String, remoteCustomer: RemoteCustomer) : RemoteCustomer
    suspend fun updateRemoteCustomer(organization:String, remoteCustomer: RemoteCustomer) : RemoteCustomer
    suspend fun deleteRemoteCustomer(organization:String, remoteCustomerId: String) : RemoteCustomer
}