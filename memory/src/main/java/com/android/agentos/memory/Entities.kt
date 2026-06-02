package com.android.agentos.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val packageName: String,
    val usageCount: Int,
    val lastUsedTimestamp: Long
)

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val graphJson: String,
    val createdTimestamp: Long
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val subgoalId: String?,
    val type: String,
    val target: String?,
    val text: String?,
    val success: Boolean,
    val timestamp: Long,
    val depth: Int = 0
)

@Entity(tableName = "failure_history")
data class FailureHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: String,
    val category: String,
    val errorType: String,
    val errorMessage: String,
    val timestamp: Long
)
