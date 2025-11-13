package com.datavite.eat.data.local.datasource

import com.datavite.eat.data.local.SyncType
import com.datavite.eat.data.local.dao.ClaimDao
import com.datavite.eat.data.local.model.LocalClaim
import kotlinx.coroutines.flow.Flow

class ClaimLocalDataSourceImpl(
    private val claimDao: ClaimDao
) : ClaimLocalDataSource {

    override fun getClaimsFlow(): Flow<List<LocalClaim>> {
        return claimDao.getClaimsFlow()
    }

    override suspend fun saveClaim(claim: LocalClaim) {
        claimDao.saveClaim(claim)
    }

    override suspend fun deleteClaim(id: String) {
        claimDao.deleteClaim(id)
    }

    override suspend fun saveClaims(claims: List<LocalClaim>) {
        claimDao.insertOrUpdateClaims(claims)
    }

    override suspend fun updateClaim(claim: LocalClaim) {
        claimDao.updateClaim(claim)
    }

    override suspend fun getClaimById(id: String): LocalClaim? {
        return claimDao.getClaimById(id)
    }

    override suspend fun getClaimForEmployeeOnDate(employeeId: String, date: String): LocalClaim? {
        return claimDao.getClaimForEmployeeOnDate(employeeId, date.toString())
    }

    override suspend fun getUnSyncedClaims(syncType: SyncType): List<LocalClaim> {
        return claimDao.getUnSyncedClaims(syncType)
    }

    override suspend fun getClaimsBySyncType(syncType: SyncType): List<LocalClaim> {
        return claimDao.getClaimsBySyncType(syncType)
    }

    override suspend fun markClaimAsSynced(claim: LocalClaim) {
        claimDao.markClaimAsSynced(claim)
    }

    override suspend fun markAsPendingDeletion(id: String, syncType: SyncType) {
        claimDao.markAsPendingDeletion(id, syncType)
    }

    override suspend fun updateClaimSyncType(id: String, syncType: SyncType) {
        claimDao.updateClaimSyncType(id, syncType)
    }
}
