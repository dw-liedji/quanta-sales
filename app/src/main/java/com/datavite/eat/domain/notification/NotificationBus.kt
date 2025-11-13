package com.datavite.eat.domain.notification

import kotlinx.coroutines.flow.Flow

interface NotificationBus {
    val events: Flow<NotificationEvent>
    suspend fun emit(event: NotificationEvent)
}
