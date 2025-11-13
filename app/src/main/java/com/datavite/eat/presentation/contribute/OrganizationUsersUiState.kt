package com.datavite.eat.presentation.contribute

import com.datavite.eat.domain.model.DomainOrganizationUser

sealed class OrganizationUsersUiState {
    data object Loading : OrganizationUsersUiState()
    data class Success(val organizationUsers: List<DomainOrganizationUser>) : OrganizationUsersUiState()
    data class Error(val message: String) : OrganizationUsersUiState()
}
