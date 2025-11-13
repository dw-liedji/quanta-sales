package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteCustomer
import com.datavite.eat.data.remote.service.RemoteCustomerService
import javax.inject.Inject

class CustomerRemoteDataSourceImpl @Inject constructor(
    private val remoteCustomerService: RemoteCustomerService
) : CustomerRemoteDataSource {
    override suspend fun getRemoteCustomers(organization:String): List<RemoteCustomer> {
        return remoteCustomerService.getRemoteCustomers(organization)
    }

    override suspend fun createRemoteCustomer(organization:String, remoteCustomer: RemoteCustomer): RemoteCustomer {
        return remoteCustomerService.createRemoteCustomer(organization, remoteCustomer)
    }

    override suspend fun updateRemoteCustomer(organization:String, remoteCustomer: RemoteCustomer): RemoteCustomer {
        return remoteCustomerService.updateRemoteCustomer(organization, remoteCustomer.id, remoteCustomer)
    }

  override suspend fun deleteRemoteCustomer(organization:String, remoteCustomerId: String): RemoteCustomer {
        return remoteCustomerService.deleteRemoteCustomer(organization, remoteCustomerId)
    }
}