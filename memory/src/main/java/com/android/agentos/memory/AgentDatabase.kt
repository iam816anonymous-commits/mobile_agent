package com.android.agentos.memory

import androidx.room.*

@Dao
interface AgentDao {
    @Query("SELECT * FROM habits ORDER BY usageCount DESC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("SELECT * FROM workflows")
    suspend fun getAllWorkflows(): List<WorkflowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Query("SELECT * FROM action_history ORDER BY timestamp DESC")
    suspend fun getActionHistory(): List<ActionHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionHistory(action: ActionHistoryEntity)

    @Query("SELECT * FROM failure_history ORDER BY timestamp DESC")
    suspend fun getFailureHistory(): List<FailureHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFailure(failure: FailureHistoryEntity)
}

@Database(entities = [HabitEntity::class, WorkflowEntity::class, ActionHistoryEntity::class, FailureHistoryEntity::class], version = 1)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
}
