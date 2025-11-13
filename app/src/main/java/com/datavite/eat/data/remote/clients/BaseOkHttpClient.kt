package com.datavite.eat.data.remote.clients

import okhttp3.OkHttpClient

interface BaseOkHttpClient {
     fun createOkhttp(): OkHttpClient
}