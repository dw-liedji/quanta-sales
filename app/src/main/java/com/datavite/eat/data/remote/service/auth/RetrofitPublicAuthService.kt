package com.datavite.eat.data.remote.service.auth

import com.datavite.eat.data.remote.model.auth.AuthSignInRequest
import com.datavite.eat.data.remote.model.auth.AuthSignInResponse
import com.datavite.eat.data.remote.model.auth.AuthSignUpRequest
import com.datavite.eat.data.remote.model.auth.AuthSignUpResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitPublicAuthService {

    @POST("en/api/v1/auth/jwt/create/")
    suspend fun signIn(
        @Body request: AuthSignInRequest
    ): AuthSignInResponse

    @POST("en/api/v1/auth/users/")
    suspend fun signUp(
        @Body request: AuthSignUpRequest
    ): AuthSignUpResponse

}