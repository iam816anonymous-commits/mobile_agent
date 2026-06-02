# Mobile-First Deployment Guide

Since you are running this directly on your mobile device, follow these instructions to get the agent operational.

## 1. Building the APK
If you don't have a PC with Android Studio, you can build this project directly on your Android device using **Termux**:

1. Install [Termux](https://termux.dev/) from F-Droid.
2. Setup the environment:
   ```bash
   pkg update
   pkg install openjdk-17 wget
   ```
3. Clone the repo and build:
   ```bash
   chmod +x gradlew
   ./gradlew assembleDebug
   ```
4. The APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`.

## 2. Setting Up the Local LLM (Mediapipe)
To use the local brain offline:

1. Download a compatible GGUF or Mediapipe-converted model (e.g., Gemma 2b or Llama small).
2. Move the model file to your internal storage, e.g., `/sdcard/Download/model.bin`.
3. Open the **Agent OS** app.
4. Tap the **Settings (gear icon)** in the top right.
5. Toggle **Use Local Model** to ON.
6. Enter the full path in **Local Model Path**: `/sdcard/Download/model.bin`.
7. Tap **Save & Back**.

## 3. Using Cloud Fallback
If you prefer to use the cloud while setting up:

1. Go to **Settings**.
2. Enter your **OpenAI API Key**.
3. Toggle **Use Local Model** to OFF.
4. Tap **Save & Back**.

## 4. Enabling the Agent
1. Android will prompt you to enable the **Accessibility Service**.
2. Go to **Settings > Accessibility > Installed Apps > Agent OS**.
3. Toggle **On**.
4. Return to the Dashboard and issue your first command!

## 5. Success Check
Try issuing this command:
> "Open Chrome and search for Android AI agents"

You should see the agent planning subgoals and executing gestures in real-time.
