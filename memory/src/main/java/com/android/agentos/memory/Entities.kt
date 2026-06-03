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

@Entity(tableName = "outcomes")
data class OutcomeEntity(
    @PrimaryKey val planId: String,
    val goal: String,
    val expectedOutcome: String?,
    val observedOutcome: String?,
    val success: Boolean,
    val manualEffortMinutes: Int = 0,
    val executionTimeMs: Long = 0,
    val qualityScore: Float = 0f, // 0.0 to 1.0 usefulness/quality
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_base")
data class KnowledgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePlanId: String?,
    val category: String, // Research, Summary, Fact, Habit
    val title: String,
    val content: String,
    val tags: String, // Comma-separated
    val usefulnessCount: Int = 0,
    val trustScore: Float = 1.0f,
    val lastVerifiedTimestamp: Long = System.currentTimeMillis(),
    val decayRate: Float = 0.01f,
    val isDerived: Boolean = false,
    val provenanceId: String? = null,
    val evidenceCoverage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "evidence_records")
data class EvidenceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val knowledgeId: Long,
    val claim: String,
    val evidenceContent: String,
    val sourceUrl: String?,
    val sourceApp: String?,
    val modelName: String?,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "verification_logs")
data class VerificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val knowledgeId: Long,
    val verifiedBy: String, // Model name or Tool name
    val method: String, // CROSS_CHECK, RE_SEARCH, USER_CONFIRM
    val outcome: Boolean,
    val updatedTrust: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "provider_metrics")
data class ProviderMetricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerName: String,
    val planId: String,
    val latencyMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCost: Double,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "graph_entities")
data class GraphEntityRecord(
    @PrimaryKey val id: String, // UUID
    val type: String, // e.g. Person, Place, Event, App
    val name: String,
    val propertiesJson: String, // Dynamic JSON properties
    val confidence: Float,
    val source: String?,
    val trustScore: Float = 1.0f,
    val lastVerifiedTimestamp: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "curation_history")
data class CurationEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // MERGE, ARCHIVE, CONTRADICTION_RESOLVED
    val description: String,
    val affectedEntityIds: String, // Comma-separated
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "graph_relationships")
data class GraphRelationshipRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromEntityId: String,
    val toEntityId: String,
    val relationshipType: String, // e.g. OWNS, USES, AT_LOCATION
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
