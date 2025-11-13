package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteHoliday
import com.datavite.eat.data.remote.service.HolidayService
import javax.inject.Inject

class HolidayRemoteDataSourceImpl @Inject constructor(
    private val holidayService: HolidayService
) : HolidayRemoteDataSource {
    override suspend fun getHolidays(organization:String): List<RemoteHoliday> {
        return holidayService.getHolidays(organization)
    }

    override suspend fun createHoliday(organization:String, holiday: RemoteHoliday) : RemoteHoliday {
        TODO("Not yet implemented")
    }

    override suspend fun updateHoliday(organization:String, holiday: RemoteHoliday) : RemoteHoliday {
        return holidayService.updateHoliday(organization=organization, holiday.id, holiday)
    }

    override suspend fun deleteHoliday(organization:String, holiday: RemoteHoliday) : RemoteHoliday{
        return holidayService.deleteHoliday(organization, holiday.id, holiday)
    }
}