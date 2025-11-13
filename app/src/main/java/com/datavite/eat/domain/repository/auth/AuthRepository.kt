package com.datavite.eat.domain.repository.auth

import com.datavite.eat.data.remote.model.auth.AuthSignInResponse
import com.datavite.eat.data.remote.model.auth.AuthSignUpRequest
import com.datavite.eat.data.remote.model.auth.AuthSignUpResponse
import com.datavite.eat.data.remote.clients.Response

interface AuthRepository {
    suspend fun signUp(authSignUpRequest: AuthSignUpRequest): Response<AuthSignUpResponse>
    suspend fun signIn(email:String, password:String): Response<AuthSignInResponse>
}