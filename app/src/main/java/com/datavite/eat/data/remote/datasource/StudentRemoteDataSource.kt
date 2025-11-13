package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteStudent

interface StudentRemoteDataSource {
    suspend fun getRemoteStudents(organization:String): List<RemoteStudent>
    suspend fun createRemoteStudent(organization:String, remoteStudent: RemoteStudent): RemoteStudent
    suspend fun updateRemoteStudent(organization:String, remoteStudent: RemoteStudent) : RemoteStudent
    suspend fun deleteRemoteStudent(organization:String, remoteStudent: RemoteStudent) : RemoteStudent
}