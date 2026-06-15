package com.example.data.repository

import com.example.data.database.AssistantDao
import com.example.data.database.KnowledgeEntity
import com.example.data.database.LogEntity
import com.example.data.database.SmartHomeEntity
import com.example.data.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {

    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val recentLogs: Flow<List<LogEntity>> = dao.getRecentHistory()
    val smartDevices: Flow<List<SmartHomeEntity>> = dao.getSmartDevices()
    val allKnowledge: Flow<List<KnowledgeEntity>> = dao.getAllKnowledge()

    suspend fun insertTask(task: TaskEntity) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        dao.updateTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        dao.deleteTaskById(id)
    }

    suspend fun insertLog(log: LogEntity) {
        dao.insertLog(log)
    }

    suspend fun clearHistory() {
        dao.clearAllLogs()
    }

    suspend fun matchOfflineRagKnowledge(query: String): String? {
        val words = query.lowercase().split(" ", ",", ".", "?", "!")
        for (word in words) {
            if (word.length > 3) {
                // Check if we can find a exact matching keyword
                val match = dao.findKnowledge(word)
                if (match != null) {
                    return match.definition
                }
            }
        }
        return null
    }

    suspend fun updateSmartDevice(device: SmartHomeEntity) {
        dao.updateDevice(device)
    }

    suspend fun seedDatabase() {
        // Only seed if empty
        val devices = listOf(
            SmartHomeEntity("living_light", "Living Room Main Light", "light", "OFF", "Living Room"),
            SmartHomeEntity("bedroom_fan", "Bedroom Ceiling Fan", "fan", "OFF", "Bedroom"),
            SmartHomeEntity("ac_module", "Climate Control A/C", "ac", "OFF", "Living Room"),
            SmartHomeEntity("kitchen_kettle", "Smart Instant Kettle", "heater", "OFF", "Kitchen")
        )
        dao.insertDevices(devices)

        val knowledge = listOf(
            KnowledgeEntity("gravity", "Gravity is a fundamental natural force where objects with mass/energy attract each other locally."),
            KnowledgeEntity("speed of light", "The speed of light in a vacuum is exactly 299,792,458 meters per second offline."),
            KnowledgeEntity("capital of france", "Paris is the historical capital of France, globally situated on the peaceful Seine River bank."),
            KnowledgeEntity("offline maps", "Local offline vector tiles calculated on-device using quantized compression parameters."),
            KnowledgeEntity("smart home", "Local smart home nodes linked over zero-cloud local LAN/WLAN using pre-authenticated WebSocket client handshakes.")
        )
        dao.insertKnowledge(knowledge)
    }
}
