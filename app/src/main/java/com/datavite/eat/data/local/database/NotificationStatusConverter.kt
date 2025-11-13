package com.datavite.eat.data.local.database

import androidx.room.TypeConverter
import com.datavite.eat.data.local.model.NotificationStatus

class NotificationStatusConverter {
    @TypeConverter
    fun fromStatus(status: NotificationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): NotificationStatus = NotificationStatus.valueOf(value)
}
