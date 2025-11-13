package com.datavite.eat.presentation.holiday

import com.datavite.eat.domain.model.DomainHoliday

enum class ATTENDANCE_ACTIONS {
    CHECK_IN,
    CHECK_OUT
}
sealed class HolidaysUiState {
    data object Loading : HolidaysUiState()
    data class Success(val holidays: List<DomainHoliday>) : HolidaysUiState()
    data class Error(val message: String) : HolidaysUiState()
}
