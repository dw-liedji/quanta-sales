package com.datavite.eat.data.notification

import javax.inject.Inject

class NotificationOrchestrator @Inject constructor(
    private val notificationServices: Array<out NotificationService>,
) {
    suspend fun notify(organization: String) {
        for (notification in notificationServices)
            notification.notify(organization)
    }
}