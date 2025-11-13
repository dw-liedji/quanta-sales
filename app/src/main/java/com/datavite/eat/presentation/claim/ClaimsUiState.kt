package com.datavite.eat.presentation.claim

import com.datavite.eat.domain.model.DomainClaim

enum class CLAIM_ACTIONS {
    CREATE,
    APPROVE,
    REJECT,
    EDIT,
}
sealed class ClaimsUiState {
    data object Loading : ClaimsUiState()
    data class Success(val claims: List<DomainClaim>) : ClaimsUiState()
    data class Error(val message: String) : ClaimsUiState()
}
