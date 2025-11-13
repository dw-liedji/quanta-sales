package com.datavite.eat.data.remote.service.auth

import com.datavite.eat.data.remote.model.auth.AuthOrgUser
import com.datavite.eat.data.remote.model.auth.AuthOrgUserRequest
import com.datavite.eat.data.remote.model.auth.AuthUser
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RetrofitPrivateAuthService {

    @GET("en/api/v1/auth/users/me/")
    suspend fun getUser(): AuthUser

    @POST("en/api/v1/auth/org/verify/")
    suspend fun authOrgUser(
        @Body request: AuthOrgUserRequest
    ): AuthOrgUser

}