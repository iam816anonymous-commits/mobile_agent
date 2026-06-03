# Architecture: Android Local Agent OS

## Overview
A local-first agentic operating system layer for Android that autonomously controls applications through Accessibility Services.

```mermaid
graph TD
    User([User Intent]) --> Dashboard[UI Dashboard]
    Dashboard --> Planner[Hierarchical Planner]

    subgraph ":ai Module"
        Planner --> Router[Intelligence Router]
        Router --> LocalLLM[Mediapipe / Gemma]
        Router --> CloudLLM[Generic Cloud / BYOK]
        Planner --> Decomposer[Goal Decomposer]
        Planner --> Evaluator[Quality Evaluator]
    end

    subgraph ":core Module"
        Planner --> Executor[Agent Executor]
        Executor --> Reflection[Reflection Engine]
        Executor --> WorldModel[World Model]
    end

    subgraph ":accessibility Module"
        Executor --> Bridge[Agent Bridge]
        Bridge --> Service[Accessibility Service]
        Service --> AndroidOS[Android UI System]
        AndroidOS -- Screen Tree --> Service
    end

    subgraph ":memory Module"
        Service --> Memory[Room Persistence]
        Memory --> Episodic[Episodic Memory]
        Memory --> Graph[Knowledge Graph]
        Memory --> Budget[Cognitive Budgeter]
    end

    subgraph ":tools Module"
        Executor --> ToolRegistry[Tool Registry]
        ToolRegistry --> Chrome[Chrome Tool]
        ToolRegistry --> Keep[Keep Tool]
        ToolRegistry --> YouTube[YouTube Tool]
    end
```

## Modular Responsibilities

### 1. :ai (Intelligence Layer)
- **Planning:** Converts natural language into hierarchical subgoal trees.
- **Routing:** Dynamically selects the best LLM based on privacy, cost, and complexity.
- **Reasoning:** Local-first inference using Mediapipe GenAI.

### 2. :accessibility (Control Layer)
- **Interaction:** Executes clicks, scrolls, and text input.
- **Understanding:** Flattens the Accessibility Node Tree into a semantic UI model.
- **Stability:** Implements strict node recycling to prevent system leaks.

### 3. :core (Engine Layer)
- **Execution:** Manages the **Plan → Execute → Observe → Verify → Reflect** feedback loop.
- **Belief Revision:** Dynamically updates trust scores based on new evidence.
- **Economics:** Tracks time saved and ROI for agentic workflows.

### 4. :memory (Persistence Layer)
- **Graph:** Stores long-term entities and relationships.
- **Episodes:** Retains successful interaction patterns for future reuse.
- **Integrity:** Tracks provenance and evidence for every knowledge claim.

### 5. :tools (Integration Layer)
- **Specialization:** Provides app-specific logic and success verification for target applications.

### 6. :app (Interface Layer)
- **Transparency:** Displays live logs, cognitive health, and strategic intelligence dashboards.
