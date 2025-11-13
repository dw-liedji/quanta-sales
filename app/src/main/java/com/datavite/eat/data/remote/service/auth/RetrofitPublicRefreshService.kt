package com.datavite.eat.data.remote.service.auth

import com.datavite.eat.data.remote.model.auth.AuthRefreshRequest
import com.datavite.eat.data.remote.model.auth.AuthRefreshResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitPublicRefreshService {
    @POST("en/api/v1/auth/jwt/refresh/")
    suspend fun refresh(
        @Body request: AuthRefreshRequest
    ): AuthRefreshResponse
}