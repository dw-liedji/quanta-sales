package com.datavite.eat.data.local.database

import androidx.room.TypeConverter
import com.datavite.eat.data.sync.EntityType
import com.datavite.eat.data.sync.OperationType

class DatabaseConverters {

    // For PendingOperationEntityType enum
    @TypeConverter
    fun stringToEntityType(value: String): EntityType {
        return enumValueOf(value)
    }

    @TypeConverter
    fun entityTypeToString(type: EntityType): String {
        return type.name
    }

    // For PendingOperationType enum (if needed)
    @TypeConverter
    fun stringToOperationType(value: String): OperationType {
        return enumValueOf(value)
    }

    @TypeConverter
    fun operationTypeToString(type: OperationType): String {
        return type.name
    }
}