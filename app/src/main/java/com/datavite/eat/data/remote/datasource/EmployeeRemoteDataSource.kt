package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteEmployee

interface EmployeeRemoteDataSource {
    suspend fun getEmployees(organization:String): List<RemoteEmployee>
    suspend fun createEmployee(organization:String, employee: RemoteEmployee): RemoteEmployee
    suspend fun updateEmployee(organization:String, employee: RemoteEmployee) : RemoteEmployee
    suspend fun deleteEmployee(organization:String, employee: RemoteEmployee) : RemoteEmployee
}