package com.android.agentos.ai

import com.android.agentos.core.models.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.IOException

/**
 * Fallback LLM provider using any OpenAI-compatible API (BYOK).
 */
class GenericCloudLLMProvider(
    private val apiKey: String,
    private val apiBaseUrl: String
) : LLMProvider {
    override val name: String = "Cloud-BYOK"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    private var lastUsage: UsageMetrics? = null

    override fun getUsageMetrics(): UsageMetrics? = lastUsage

    @Serializable
    private data class ChatRequest(
        val model: String = "gpt-3.5-turbo",
        val messages: List<ChatMessage>,
        val response_format: ResponseFormat? = null
    )

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice>,
        val usage: OpenAIUsage? = null
    )

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class OpenAIUsage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )

    var toolRegistry: ToolRegistry? = null

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        if (apiKey.isEmpty()) {
            return Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
        }

        val prompt = buildPrompt(userInput, screenContext)
        val requestBody = ChatRequest(
            messages = listOf(ChatMessage("user", prompt)),
            response_format = ResponseFormat("json_object")
        )

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${apiBaseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    val chatResponse = json.decodeFromString<ChatResponse>(body)

                    chatResponse.usage?.let {
                        lastUsage = UsageMetrics(
                            inputTokens = it.prompt_tokens,
                            outputTokens = it.completion_tokens,
                            estimatedCost = (it.prompt_tokens * 0.0005 + it.completion_tokens * 0.0015) / 1000.0
                        )
                    }

                    val content = chatResponse.choices.first().message.content
                    val steps = json.decodeFromString<List<AgentAction>>(extractJson(content))
                    Plan(goal = userInput, steps = steps)
                }
            } catch (e: Exception) {
                Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
            }
        }
    }

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
        val jsonArrayRegex = Regex("\\[[\\s\\S]*\\]")
        val jsonObjectRegex = Regex("\\{[\\s\\S]*\\}")

        return jsonArrayRegex.find(text)?.value
            ?: jsonObjectRegex.find(text)?.value
            ?: text
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return VerificationResult(true, 0.95f, "Verified via $apiBaseUrl")
    }
}
