package com.datavite.eat.domain.repository.auth

import com.datavite.eat.data.remote.model.auth.AuthOrgUser
import com.datavite.eat.data.remote.model.auth.AuthOrgUserRequest
import com.datavite.eat.data.remote.model.auth.AuthUser
import com.datavite.eat.data.remote.clients.Response

interface UserRepository {
    suspend fun getUserProfile(): Response<AuthUser>
    suspend fun authOrgUser(request: AuthOrgUserRequest): Response<AuthOrgUser>

}