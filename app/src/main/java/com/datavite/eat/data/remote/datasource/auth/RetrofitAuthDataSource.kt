package com.datavite.eat.data.remote.datasource.auth

import android.util.Log
import com.datavite.eat.data.remote.clients.Response
import com.datavite.eat.data.remote.model.auth.AuthSignInRequest
import com.datavite.eat.data.remote.model.auth.AuthSignInResponse
import com.datavite.eat.data.remote.model.auth.AuthSignUpRequest
import com.datavite.eat.data.remote.model.auth.AuthSignUpResponse
import com.datavite.eat.data.remote.service.auth.RetrofitPublicAuthService
import retrofit2.HttpException
import javax.inject.Inject


class RetrofitAuthDataSource @Inject constructor (
    private val authRetrofitService: RetrofitPublicAuthService
): RemoteAuthDataSource {

    override suspend fun signUp(authSignUpRequest: AuthSignUpRequest) : Response<AuthSignUpResponse> {
        return try {
            val response = authRetrofitService.signUp(authSignUpRequest)
            signIn(email = authSignUpRequest.email, password = authSignUpRequest.password)
            Response.Authorized(data = response)
        }catch (e: HttpException){
            if (e.code() == 401) {
                Response.UnknownError(errorMsg = "error")
            }else {
                Response.UnknownError("error")
            }
        }catch (e:Exception){
            Response.UnknownError("erro")
        }
    }

    override suspend fun signIn(email: String, password: String): Response<AuthSignInResponse> {
        return try {
            Log.i("Retrofit:", "Data source starting retrofit request")

            val response = authRetrofitService.signIn(AuthSignInRequest(email=email, password=password))

            Response.Authorized(data = response)
        }catch (e: HttpException){
            if (e.code() == 401) {
                e.printStackTrace()
                Log.i("Retrofit:", "Data source request unAuthorized")
                Response.UnknownError("error ")
            }else {
                e.printStackTrace()
                Log.i("Retrofit:", "Data source request error")
                Response.UnknownError("error ")
            }
        }catch (e:Exception){
            e.printStackTrace()
            Response.UnknownError("error ")
        }
    }

}