package com.datavite.eat.domain.repository

import com.datavite.eat.domain.model.DomainHoliday
import kotlinx.coroutines.flow.Flow

interface HolidayRepository {

    suspend fun getHolidayById(id: String): DomainHoliday?
    suspend fun getHolidaysFlow(): Flow<List<DomainHoliday>>
    suspend fun getHolidayForDate(date: String): DomainHoliday?

    suspend fun createHoliday(organization: String, holiday: DomainHoliday)
    suspend fun updateHoliday(organization: String, holiday: DomainHoliday)
    suspend fun deleteHoliday(organization: String, holiday: DomainHoliday)
    suspend fun syncHolidays(organization: String)
}