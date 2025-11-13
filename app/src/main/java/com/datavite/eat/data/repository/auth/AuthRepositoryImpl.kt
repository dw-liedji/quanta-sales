package com.datavite.eat.data.repository.auth

import android.util.Log
import com.datavite.eat.data.remote.clients.Response
import com.datavite.eat.data.remote.datasource.auth.RemoteAuthDataSource
import com.datavite.eat.data.remote.model.auth.AuthSignInResponse
import com.datavite.eat.data.remote.model.auth.AuthSignUpRequest
import com.datavite.eat.data.remote.model.auth.AuthSignUpResponse
import com.datavite.eat.domain.repository.auth.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor (
    private val remoteAuthDataSource: RemoteAuthDataSource
): AuthRepository {

    override suspend fun signUp(authSignUpRequest: AuthSignUpRequest): Response<AuthSignUpResponse> {
        Log.i("Retrofit:", "repository")
       return remoteAuthDataSource.signUp(authSignUpRequest=authSignUpRequest)
    }
    override suspend fun signIn(email: String, password: String): Response<AuthSignInResponse> {
        return remoteAuthDataSource.signIn(email=email, password=password)
    }

}