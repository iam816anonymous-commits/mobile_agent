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

    @Query("SELECT * FROM knowledge_base ORDER BY timestamp DESC")
    suspend fun getAllKnowledge(): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_base WHERE content LIKE :query OR tags LIKE :query")
    suspend fun searchKnowledge(query: String): List<KnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeEntity)

    @Query("SELECT * FROM graph_entities WHERE name LIKE :query")
    suspend fun searchGraphEntities(query: String): List<GraphEntityRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphEntity(entity: GraphEntityRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: GraphRelationshipRecord)

    @Query("SELECT * FROM graph_relationships WHERE fromEntityId = :entityId OR toEntityId = :entityId")
    suspend fun getRelationships(entityId: String): List<GraphRelationshipRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurationEvent(event: CurationEvent)

    @Query("SELECT * FROM curation_history ORDER BY timestamp DESC")
    suspend fun getCurationHistory(): List<CurationEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceRecord)

    @Query("SELECT * FROM evidence_records WHERE knowledgeId = :knowledgeId")
    suspend fun getEvidenceForKnowledge(knowledgeId: Long): List<EvidenceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(log: VerificationLog)

    @Query("SELECT * FROM verification_logs WHERE knowledgeId = :knowledgeId ORDER BY timestamp DESC")
    suspend fun getVerificationHistory(knowledgeId: Long): List<VerificationLog>
}

@Database(entities = [HabitEntity::class, WorkflowEntity::class, ActionHistoryEntity::class, FailureHistoryEntity::class, OutcomeEntity::class, ProviderMetricsEntity::class, KnowledgeEntity::class, GraphEntityRecord::class, GraphRelationshipRecord::class, CurationEvent::class, EvidenceRecord::class, VerificationLog::class], version = 6)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
}
