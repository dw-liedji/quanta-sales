package com.datavite.eat.data.notification

import com.datavite.eat.domain.notification.NotificationBus
import com.datavite.eat.domain.notification.NotificationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class NotificationBusImpl : NotificationBus {

    private val _events = MutableSharedFlow<NotificationEvent>()
    override val events: Flow<NotificationEvent> = _events

    override suspend fun emit(event: NotificationEvent) {
        _events.emit(event)
    }
}
