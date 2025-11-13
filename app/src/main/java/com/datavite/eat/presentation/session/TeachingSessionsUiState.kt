package com.datavite.eat.presentation.session

import com.datavite.eat.domain.model.DomainTeachingSession

sealed class TeachingSessionsUiState {
    data object Loading : TeachingSessionsUiState()
    data class Success(val teachingSessions: List<DomainTeachingSession>) : TeachingSessionsUiState()
    data class Error(val message: String) : TeachingSessionsUiState()
}
