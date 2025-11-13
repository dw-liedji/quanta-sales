package com.datavite.eat.presentation.teachingcourse

import com.datavite.eat.domain.model.DomainTeachingCourse

sealed class TeachingCoursesUiState {
    data object Loading : TeachingCoursesUiState()
    data class Success(val teachingCourses: List<DomainTeachingCourse>) : TeachingCoursesUiState()
    data class Error(val message: String) : TeachingCoursesUiState()
}
