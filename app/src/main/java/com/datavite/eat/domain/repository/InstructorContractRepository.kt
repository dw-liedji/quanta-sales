package com.datavite.eat.domain.repository

import com.datavite.eat.domain.model.DomainInstructorContract
import kotlinx.coroutines.flow.Flow

interface InstructorContractRepository {

    suspend fun getDomainInstructorContractById(id: String): DomainInstructorContract?
    fun searchDomainInstructorContractsFor(searchQuery: String): List<DomainInstructorContract>
    suspend fun getDomainInstructorContractsFlow(): Flow<List<DomainInstructorContract>>
    suspend fun createDomainInstructorContract(organization: String, domainInstructorContract: DomainInstructorContract)
    suspend fun updateDomainInstructorContract(organization: String, domainInstructorContract: DomainInstructorContract)
    suspend fun deleteDomainInstructorContract(organization: String, domainInstructorContract: DomainInstructorContract)
    suspend fun syncLocalWithRemoteInstructorContracts(organization: String)
}