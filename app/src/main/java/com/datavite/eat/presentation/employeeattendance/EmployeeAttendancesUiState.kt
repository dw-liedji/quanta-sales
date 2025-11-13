package com.datavite.eat.presentation.employeeattendance

import com.datavite.eat.domain.model.DomainStudentAttendance

enum class ATTENDANCE_ACTIONS {
    CHECK_IN,
    CHECK_OUT
}
sealed class EmployeeAttendancesUiState {
    data object Loading : EmployeeAttendancesUiState()
    data class Success(val attendances: List<DomainStudentAttendance>) : EmployeeAttendancesUiState()
    data class Error(val message: String) : EmployeeAttendancesUiState()
}
