package com.datavite.eat.presentation.instructorContract

import com.datavite.eat.domain.model.DomainInstructorContract

sealed class InstructorContractsUiState {
    data object Loading : InstructorContractsUiState()
    data class Success(val domainInstructorContracts: List<DomainInstructorContract>) : InstructorContractsUiState()
    data class Error(val message: String) : InstructorContractsUiState()
}
