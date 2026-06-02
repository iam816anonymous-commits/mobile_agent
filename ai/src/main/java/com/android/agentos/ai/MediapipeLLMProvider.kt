package com.android.agentos.ai

import android.content.Context
import com.android.agentos.core.models.*
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.serialization.json.Json
import java.io.File

class MediapipeLLMProvider(private val context: Context, private val modelPath: String) : LLMProvider {
    override val name: String = "Mediapipe-Local-LLM"

    private val llmInference: LlmInference by lazy {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
            .build()
        LlmInference.createFromOptions(context, options)
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        val prompt = buildPrompt(userInput, screenContext)
        val response = llmInference.generateResponse(prompt)

        return try {
            // Expecting model to output JSON plan
            val steps = json.decodeFromString<List<AgentAction>>(extractJson(response))
            Plan(goal = userInput, steps = steps)
        } catch (e: Exception) {
            // Fallback if model fails to output valid JSON
            Plan(goal = userInput, steps = listOf(AgentAction(ActionType.WAIT)), status = PlanStatus.FAILED)
        }
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        val prompt = "Action: ${action.type} on ${action.target}. Screen Context: ${screenContext.take(5)}. Did it succeed? Return JSON: { \"success\": boolean, \"confidence\": float, \"reason\": string }"
        val response = llmInference.generateResponse(prompt)
        return try {
            json.decodeFromString<VerificationResult>(extractJson(response))
        } catch (e: Exception) {
            VerificationResult(true, 0.5f, "Verification parsing failed")
        }
    }

    var toolRegistry: ToolRegistry? = null

    private fun buildPrompt(userInput: String, screenContext: List<ScreenElement>): String {
        val schema = """
            [
              { "type": "CLICK", "target": "string", "text": "string", "x": int, "y": int },
              ...
            ]
        """.trimIndent()
        val toolsMetadata = toolRegistry?.getToolMetadata() ?: "Standard Android Actions"

        return """
            System: You are a professional Android agent.
            Goal: $userInput

            Available Tools & Capabilities:
            $toolsMetadata

            Global Actions: OPEN_APP, CLICK, TYPE_TEXT, SCROLL_UP, SCROLL_DOWN, GO_BACK, WAIT, VERIFY_ELEMENT

            Current Screen Elements: ${screenContext.take(15).map { it.text ?: it.contentDescription }}

            Output ONLY a JSON array of actions following this schema:
            $schema
        """.trimIndent()
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf("[")
        val end = text.lastIndexOf("]")
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1)
        }
        return text
    }
}
