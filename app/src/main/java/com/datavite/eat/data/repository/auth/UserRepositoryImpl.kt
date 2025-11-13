package com.datavite.eat.data.repository.auth

import com.datavite.eat.data.remote.datasource.auth.RemoteUserDataSource
import com.datavite.eat.data.remote.model.auth.AuthOrgUser
import com.datavite.eat.data.remote.model.auth.AuthOrgUserRequest
import com.datavite.eat.data.remote.model.auth.AuthUser
import com.datavite.eat.domain.repository.auth.UserRepository
import com.datavite.eat.data.remote.clients.Response
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor (private val remoteUserDataSource: RemoteUserDataSource) : UserRepository {
    override suspend fun getUserProfile(): Response<AuthUser> {
        return remoteUserDataSource.getUserProfile()
    }

    override suspend fun authOrgUser(request: AuthOrgUserRequest): Response<AuthOrgUser> {
        return remoteUserDataSource.authOrgUser(request)
    }
}