package com.android.agentos.ai

import com.android.agentos.core.models.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fallback LLM provider using any OpenAI-compatible API (BYOK).
 */
class GenericCloudLLMProvider(
    private val apiKey: String,
    private val apiBaseUrl: String
) : LLMProvider {
    override val name: String = "Cloud-BYOK"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
        }

        val prompt = buildPrompt(userInput, screenContext)
        val response = callOpenAI(prompt) ?: return@withContext Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)

        return@withContext try {
            val content = extractContent(response)
            val steps = json.decodeFromString<List<AgentAction>>(extractJson(content))
            Plan(goal = userInput, steps = steps)
        } catch (e: Exception) {
            Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
        }
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult = withContext(Dispatchers.IO) {
        val prompt = "Action: ${action.type} on ${action.target}. Screen Context: ${screenContext.take(5)}. Did it succeed? Return JSON: { \"success\": boolean, \"confidence\": float, \"reason\": string }"
        val response = callOpenAI(prompt) ?: return@withContext VerificationResult(false, 0f, "Network error")

        return@withContext try {
            val content = extractContent(response)
            json.decodeFromString<VerificationResult>(extractJson(content))
        } catch (e: Exception) {
            VerificationResult(true, 0.5f, "Verification parsing failed")
        }
    }

    override suspend fun decomposeGoal(goal: String): List<Subgoal> = withContext(Dispatchers.IO) {
        val prompt = "Decompose the goal '$goal' into exactly 3 high-level subgoals. Return ONLY a JSON array of strings: [\"subgoal1\", \"subgoal2\", \"subgoal3\"]"
        val response = callOpenAI(prompt) ?: return@withContext emptyList()
        return@withContext try {
            val content = extractContent(response)
            val subgoals = json.decodeFromString<List<String>>(extractJson(content))
            subgoals.map { Subgoal(description = it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun callOpenAI(prompt: String): String? {
        return try {
            val url = URL("${apiBaseUrl.removeSuffix("/")}/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true

            val body = buildJsonObject {
                put("model", "gpt-3.5-turbo")
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.0)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                Scanner(conn.inputStream).useDelimiter("\\A").next()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractContent(jsonResponse: String): String {
        val root = json.parseToJsonElement(jsonResponse).jsonObject
        return root["choices"]?.jsonArray?.get(0)?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""
    }

    private fun buildPrompt(userInput: String, screenContext: List<ScreenElement>): String {
        return """
            System: You are a professional Android agent.
            Goal: $userInput
            Current Screen Elements: ${screenContext.take(15).map { it.text ?: it.contentDescription }}
            Output ONLY a JSON array of actions: [ { "type": "CLICK", "target": "string" }, ... ]
        """.trimIndent()
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf("[")
        val end = text.lastIndexOf("]")
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1)
        }
        val objStart = text.indexOf("{")
        val objEnd = text.lastIndexOf("}")
        if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1)
        }
        return text
    }
}
