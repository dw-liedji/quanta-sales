package com.datavite.eat.data.remote.datasource

import android.os.Build
import com.datavite.eat.data.remote.model.RemoteBilling
import com.datavite.eat.data.remote.service.RemoteBillingService
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class BillingRemoteDataSourceImpl @Inject constructor(
    private val remoteBillingService: RemoteBillingService
) : BillingRemoteDataSource {
    override suspend fun getRemoteBillings(organization:String): List<RemoteBilling> {
        return remoteBillingService.getRemoteBillings(organization)
    }

    override suspend fun getRemoteBillingsChangesSince(
        organization: String,
        since: Long
    ): List<RemoteBilling> {
        // Convert Long timestamp to Django datetime string
        return remoteBillingService.getRemoteBillingsChangesSince(organization, since)
    }


    override suspend fun createRemoteBilling(organization:String, remoteBilling: RemoteBilling): RemoteBilling {
        return remoteBillingService.createRemoteBilling(organization, remoteBilling)
    }

    override suspend fun updateRemoteBilling(organization:String, remoteBilling: RemoteBilling): RemoteBilling {
        return remoteBillingService.updateRemoteBilling(organization, remoteBilling.id, remoteBilling)
    }

  override suspend fun deleteRemoteBilling(organization:String, remoteBillingId: String): RemoteBilling {
        return remoteBillingService.deleteRemoteBilling(organization, remoteBillingId)
    }
}