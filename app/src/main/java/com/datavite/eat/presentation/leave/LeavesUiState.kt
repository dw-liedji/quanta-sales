package com.datavite.eat.presentation.leave

import com.datavite.eat.domain.model.DomainLeave

enum class LEAVE_ACTIONS {
    CREATE,
    APPROVE,
    REJECT,
}
sealed class LeavesUiState {
    data object Loading : LeavesUiState()
    data class Success(val leaves: List<DomainLeave>) : LeavesUiState()
    data class Error(val message: String) : LeavesUiState()
}
