package com.datavite.eat.data.remote.datasource

import com.datavite.eat.data.remote.model.RemoteRoom


interface RoomRemoteDataSource {
    suspend fun getRooms(organization:String): List<RemoteRoom>
    suspend fun createRoom(organization:String, room: RemoteRoom)
    suspend fun updateRoom(organization:String, room: RemoteRoom)
    suspend fun deleteRoom(organization:String, roomId: String)
}