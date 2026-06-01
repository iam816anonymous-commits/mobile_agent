# Android Local Agent OS

A production-grade, local-first Android agent that understands the screen and controls apps through Accessibility Services.

## Architecture

The system is built with a modular architecture:

- **`:app`**: Main Android application, UI, and entry points.
- **`:ai`**: Planning engine, reasoning, and local LLM integration.
- **`:accessibility`**: Core Accessibility Service, screen parsing, and action execution.
- **`:tools`**: Extensible tool system for interacting with specific apps.
- **`:memory`**: Persistence layer using Room (SQLite) and local vector storage.

## Core Modules

### 1. Local Brain
- **Intent Detection**: Analyzes user input to determine the goal.
- **Planner**: Generates a multi-step execution plan.
- **Reflection**: Monitors execution and adjusts the plan if needed.

### 2. Accessibility Controller
- Uses Android's `AccessibilityService` to:
    - Click UI elements.
    - Type text.
    - Scroll.
    - Navigate.
    - Extract screen hierarchy (Accessibility Tree).

### 3. Screen Understanding
- Parses the Accessibility Tree to identify interactive elements.
- Fallback to OCR for non-standard UI components.
- Screenshot analysis for visual verification.

### 4. Memory System
- **Short-term**: Current task context and recent actions.
- **Long-term**: User preferences and learned habits.
- **Workflows**: Recorded sequences of actions.

## Getting Started

### Prerequisites
- Android SDK (API 33+)
- Gradle 8.0+
- JDK 17+

### Build
```bash
./gradlew assembleDebug
```

## Safety & Privacy
- All processing is local.
- Safety layer prevents sensitive actions (payments, etc.) without explicit confirmation.
