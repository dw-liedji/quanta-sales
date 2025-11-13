package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteClaim

interface ClaimRemoteDataSource {
    suspend fun getClaims(organization:String): List<RemoteClaim>
    suspend fun createClaim(organization:String, claim: RemoteClaim): RemoteClaim
    suspend fun updateClaim(organization:String, claim: RemoteClaim) : RemoteClaim
    suspend fun deleteClaim(organization:String, claim: RemoteClaim) : RemoteClaim
}