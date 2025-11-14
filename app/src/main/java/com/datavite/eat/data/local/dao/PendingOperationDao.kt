package com.datavite.eat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.datavite.eat.data.local.model.PendingOperation
import kotlinx.coroutines.flow.Flow
@Dao
interface PendingOperationDao {

    @Query("SELECT failedAttempts FROM pending_operations WHERE id = :operationId")
    suspend fun getFailureCount(operationId: Long): Int
    @Insert
    suspend fun insert(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getNextOperations(limit: Int = 50): List<PendingOperation>

    @Query("""
        SELECT * FROM pending_operations 
        WHERE failedAttempts < :maxRetries 
        ORDER BY createdAt ASC LIMIT :limit
    """)
    suspend fun getNextOperationsEligibleForRetry(limit: Int = 50, maxRetries: Int = 5): List<PendingOperation>

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_operations SET failedAttempts = failedAttempts + 1 WHERE id = :id")
    suspend fun incrementFailureCount(id: Long)

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun getAllPendingOperationsFlow(): Flow<List<PendingOperation>>

}
