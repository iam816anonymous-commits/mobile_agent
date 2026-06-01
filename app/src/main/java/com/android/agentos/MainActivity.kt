package com.android.agentos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.agentos.ai.Planner
import com.android.agentos.ai.RuleBasedLLMProvider
import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.PlanStatus
import com.android.agentos.core.models.ExecutionResult
import com.android.agentos.core.engine.Executor
import com.android.agentos.accessibility.AgentAccessibilityService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val planner = Planner(RuleBasedLLMProvider())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgentDashboard(planner)
                }
            }
        }
    }
}

@Composable
fun AgentDashboard(planner: Planner) {
    var command by remember { mutableStateOf("") }
    var currentPlan by remember { mutableStateOf<Plan?>(null) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Agent OS Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Command the Agent") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    logs = logs + "Planning: $command"
                    val service = AgentAccessibilityService.instance
                    val screenContext = service?.getCurrentScreenHierarchy() ?: emptyList()
                    val plan = planner.generatePlan(command, screenContext)
                    currentPlan = plan
                    logs = logs + "Plan generated: ${plan.steps.size} steps"

                    if (service != null) {
                        val executor = Executor(
                            accessibilityProvider = service,
                            verificationProvider = service,
                            onActionStarted = { action ->
                                logs = logs + "Starting: ${action.type}"
                            },
                            onActionFinished = { result ->
                                logs = logs + "Finished: ${result.message}"
                            },
                            onFailure = { failure ->
                                logs = logs + "Error: ${failure.errorMessage}"
                            }
                        )
                        executor.execute(plan)
                    } else {
                        logs = logs + "Error: Accessibility Service not running"
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
        ) {
            Text("Execute Plan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        currentPlan?.let { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Plan Status: ${plan.status}", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = if (plan.status == PlanStatus.COMPLETED) 1f else 0.5f,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    plan.steps.forEach { step ->
                        Text("• ${step.type}: ${step.target ?: ""}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Action Logs", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            items(logs.reversed()) { log ->
                Text(log, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Divider()
            }
        }
    }
}
