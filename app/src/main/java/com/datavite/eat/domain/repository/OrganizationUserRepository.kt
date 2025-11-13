package com.datavite.eat.domain.repository

import com.datavite.eat.data.local.model.LocalOrganizationUser
import com.datavite.eat.domain.model.DomainOrganizationUser
import kotlinx.coroutines.flow.Flow

interface OrganizationUserRepository {
    suspend fun getUserById(id: String): LocalOrganizationUser?
    suspend fun getOrganizationUsersFlow(): Flow<List<DomainOrganizationUser>>
    suspend fun getOrgUserById(userId:String): DomainOrganizationUser?
    suspend fun createOrganizationUser(organization: String, user: DomainOrganizationUser)
    suspend fun updateOrganizationUser(organization: String, user: DomainOrganizationUser)
    suspend fun deleteOrganizationUser(organization: String, user: DomainOrganizationUser)
    suspend fun syncOrganizationUsers(organization: String)
}