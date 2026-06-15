package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "knowledge_graph")
data class KnowledgeEntity(
    @PrimaryKey val keyword: String,
    val definition: String
)

@Entity(tableName = "scheduled_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskType: String, // 'alarm', 'timer', 'reminder'
    val details: String,
    val targetTime: String, // "HH:MM" or seconds for timer
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val response: String,
    val latencyMs: Int,
    val timestamp: String,
    val isLocal: Boolean = true
)

@Entity(tableName = "smart_home_devices")
data class SmartHomeEntity(
    @PrimaryKey val deviceId: String,
    val name: String,
    val type: String, // 'light', 'fan', 'heater', 'ac'
    val stateValue: String, // 'ON', 'OFF', '50%'
    val room: String
)

@Dao
interface AssistantDao {
    // Knowledge Graph
    @Query("SELECT * FROM knowledge_graph")
    fun getAllKnowledge(): Flow<List<KnowledgeEntity>>

    @Query("SELECT * FROM knowledge_graph WHERE keyword LIKE :query OR definition LIKE :query LIMIT 1")
    suspend fun findKnowledge(query: String): KnowledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(items: List<KnowledgeEntity>)

    // Scheduled Tasks
    @Query("SELECT * FROM scheduled_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    // Command History Logs
    @Query("SELECT * FROM command_logs ORDER BY id DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM command_logs")
    suspend fun clearAllLogs()

    // Smart Home IoT Devices
    @Query("SELECT * FROM smart_home_devices")
    fun getSmartDevices(): Flow<List<SmartHomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<SmartHomeEntity>)

    @Update
    suspend fun updateDevice(device: SmartHomeEntity)
}
