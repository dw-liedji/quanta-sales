package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteHoliday

interface HolidayRemoteDataSource {
    suspend fun getHolidays(organization:String): List<RemoteHoliday>
    suspend fun createHoliday(organization:String, holiday: RemoteHoliday): RemoteHoliday
    suspend fun updateHoliday(organization:String, holiday: RemoteHoliday) : RemoteHoliday
    suspend fun deleteHoliday(organization:String, holiday: RemoteHoliday) : RemoteHoliday
}