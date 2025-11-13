package com.datavite.eat.data.remote.service

import com.datavite.eat.data.remote.model.RemoteCustomer
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RemoteCustomerService {
    @GET("en/{organization}/api/v1/data/customers/")
    suspend fun getRemoteCustomers(
        @Path("organization") organization: String, // Assuming 'modified' is a date field, adjust type accordingly
     ): List<RemoteCustomer>


    @POST("en/{organization}/api/v1/data/customers/create/")
    suspend fun createRemoteCustomer(
        @Path("organization") organization: String,
        @Body remoteRemoteCustomer: RemoteCustomer
    ): RemoteCustomer

    @PUT("en/{organization}/api/v1/data/customers/{id}/edit/")
    suspend fun updateRemoteCustomer(
        @Path("organization") organization: String,
        @Path("id") id: String,
        @Body session: RemoteCustomer
    ): RemoteCustomer

    @DELETE("en/{organization}/api/v1/data/customers/{id}/delete/")
    suspend fun deleteRemoteCustomer(
        @Path("organization") organization: String,
        @Path("id") id: String
    ): RemoteCustomer

}
