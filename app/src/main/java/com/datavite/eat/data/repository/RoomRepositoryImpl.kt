package com.datavite.eat.data.repository

import android.util.Log
import com.datavite.cameinet.feature.cameis.domain.repository.RoomRepository
import com.datavite.eat.data.mapper.RoomMapper
import com.datavite.eat.data.local.SyncType
import com.datavite.eat.data.local.datasource.RoomLocalDataSource
import com.datavite.eat.data.remote.datasource.RoomRemoteDataSource
import com.datavite.eat.domain.model.DomainRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomRepositoryImpl @Inject constructor(
    private val roomRemoteDataSource: RoomRemoteDataSource,
    private val roomLocalDataSource: RoomLocalDataSource,
    private val roomMapper: RoomMapper
) : RoomRepository {

    override suspend fun getRoomsFlow(): Flow<List<DomainRoom>> {
        return roomLocalDataSource.getRoomsFlow().map { rooms ->
            rooms.map { roomMapper.mapLocalToDomain(it) }
        }
    }

    override suspend fun getAllRooms(): List<DomainRoom> {
        return roomLocalDataSource.getAllRooms().map { roomMapper.mapLocalToDomain(it) }
    }

    override suspend fun createRoom(organization: String, room: DomainRoom) {
        try {
            val remotePeriod = roomMapper.mapDomainToRemote(room)
            roomRemoteDataSource.createRoom(organization, remotePeriod)
            val localPeriod = roomMapper.mapDomainToLocal(room, SyncType.SYNCED)
            roomLocalDataSource.saveRoom(localPeriod)
        } catch (e: Exception) {
            val localPeriod = roomMapper.mapDomainToLocal(room, SyncType.PENDING_CREATION)
            roomLocalDataSource.saveRoom(localPeriod)
        }
    }

    override suspend fun updateRoom(organization: String, room: DomainRoom) {
        try {
            val remotePeriod = roomMapper.mapDomainToRemote(room)
            roomRemoteDataSource.updateRoom(organization, remotePeriod)
            val localPeriod = roomMapper.mapDomainToLocal(room, SyncType.SYNCED)
            roomLocalDataSource.saveRoom(localPeriod)
        } catch (e: Exception) {
            val localPeriod = roomMapper.mapDomainToLocal(room, SyncType.PENDING_MODIFICATION)
            roomLocalDataSource.saveRoom(localPeriod)
        }
    }

    override suspend fun deleteRoom(organization: String, roomId: String) {
        try {
            roomRemoteDataSource.deleteRoom(organization, roomId)
            roomLocalDataSource.deleteRoom(roomId)
        } catch (e: Exception) {
            roomLocalDataSource.markAsPendingDeletion(roomId)
        }
    }

    override suspend fun syncRooms(organization: String) {
        // Fetch unSynced rooms from the local database
        val unSyncedPeriods = roomLocalDataSource.getUnSyncedPeriods()

        // Try to sync them with the server
        for (room in unSyncedPeriods) {
            try {
                when (room.syncType) {
                    SyncType.PENDING_CREATION -> createRoom(organization, roomMapper.mapLocalToDomain(room))
                    SyncType.PENDING_MODIFICATION -> {
                        // Should implement conflict resolution between remote and local changes
                        try {
                            val domainPeriod =  roomMapper.mapLocalToDomain(room)
                            val remotePeriod = roomMapper.mapDomainToRemote(domainPeriod)
                            roomRemoteDataSource.updateRoom(organization, remotePeriod)
                            val localPeriod = roomMapper.mapDomainToLocal(domainPeriod, SyncType.SYNCED)
                            roomLocalDataSource.saveRoom(localPeriod)
                        } catch (e: Exception) {
                            val domainPeriod =  roomMapper.mapLocalToDomain(room)
                            val localPeriod = roomMapper.mapDomainToLocal(domainPeriod, SyncType.PENDING_MODIFICATION)
                            roomLocalDataSource.saveRoom(localPeriod)
                        }
                    }
                    SyncType.PENDING_DELETION -> deleteRoom(organization, room.id)
                    else -> {}
                }

            } catch (e: Exception) {
                // Handle the sync failure (e.g., log error, retry later)
                Log.i("cameinet-room-log", "update failed")

            }
        }

        fetchLatestRemoteRoomsAndUpdateLocalRooms(organization)
    }

    private suspend fun fetchLatestRemoteRoomsAndUpdateLocalRooms(organization: String){
        try {
            val remotePeriods = roomRemoteDataSource.getRooms(organization)
            val domainPeriods = remotePeriods.map { roomMapper.mapRemoteToDomain(it) }
            val localPeriods = domainPeriods.map { roomMapper.mapDomainToLocal(it, SyncType.SYNCED) }
            roomLocalDataSource.saveRooms(localPeriods)
            Log.i("cameinet-room-log", "update success")

        } catch (e: Exception) {
            // If fetching from remote fails, fallback to local
            Log.i("cameinet-room-log", e.message.toString())
        }
    }

}