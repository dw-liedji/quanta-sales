package com.datavite.eat.data.notification

interface NotificationService {
    suspend fun notify(organization: String)
}