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

    @Query("SELECT * FROM outcomes ORDER BY timestamp DESC")
    suspend fun getAllOutcomes(): List<OutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutcome(outcome: OutcomeEntity)

    @Query("SELECT * FROM provider_metrics ORDER BY timestamp DESC")
    suspend fun getProviderMetrics(): List<ProviderMetricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderMetric(metric: ProviderMetricsEntity)
}

@Database(entities = [HabitEntity::class, WorkflowEntity::class, ActionHistoryEntity::class, FailureHistoryEntity::class, OutcomeEntity::class, ProviderMetricsEntity::class], version = 2)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
}
