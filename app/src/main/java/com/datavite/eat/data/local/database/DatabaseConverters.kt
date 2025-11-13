package com.datavite.eat.data.local.database

import androidx.room.TypeConverter
import com.datavite.eat.domain.PendingOperationEntityType
import com.datavite.eat.domain.PendingOperationType

class DatabaseConverters {

    // For PendingOperationEntityType enum
    @TypeConverter
    fun stringToEntityType(value: String): PendingOperationEntityType {
        return enumValueOf(value)
    }

    @TypeConverter
    fun entityTypeToString(type: PendingOperationEntityType): String {
        return type.name
    }

    // For PendingOperationType enum (if needed)
    @TypeConverter
    fun stringToOperationType(value: String): PendingOperationType {
        return enumValueOf(value)
    }

    @TypeConverter
    fun operationTypeToString(type: PendingOperationType): String {
        return type.name
    }
}