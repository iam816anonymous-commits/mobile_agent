package com.android.agentos.core.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowGraph(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val nodes: List<WorkflowNode>,
    val startNodeId: String
)

@Serializable
data class WorkflowNode(
    val id: String,
    val action: AgentAction,
    val transitions: List<WorkflowTransition>,
    val timeoutMillis: Long = 5000L,
    val maxRetries: Int = 3
)

@Serializable
data class WorkflowTransition(
    val condition: String, // e.g. "SUCCESS", "FAILURE", "SCREEN_MATCH:Search Results"
    val targetNodeId: String
)
