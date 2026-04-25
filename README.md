# Anthropic-x-Whale-Hackathon — Gemma 4 On-Device Chatbot

A minimal **Expo + React Native (Android)** chatbot that runs **Gemma 4 E2B-it** fully on-device via Google's official [LiteRT-LM](https://ai.google.dev/edge/litert/lm/android) Kotlin API (`com.google.ai.edge.litertlm:litertlm-android`).

No API calls. No keys. The `.litertlm` model file lives on the phone and inference runs in-process.

## Project layout

- [App.tsx](App.tsx) — chat UI with streaming output
- [modules/gemma-llm/index.ts](modules/gemma-llm/index.ts) — TypeScript bridge to the native module
- [modules/gemma-llm/android/src/main/java/expo/modules/gemmallm/GemmaLlmModule.kt](modules/gemma-llm/android/src/main/java/expo/modules/gemmallm/GemmaLlmModule.kt) — Kotlin module wrapping `Engine` + `Conversation`
- [modules/gemma-llm/android/build.gradle](modules/gemma-llm/android/build.gradle) — depends on `com.google.ai.edge.litertlm:litertlm-android`
- [app.json](app.json) — Expo config (uses `expo-dev-client`, Android only)

## Prerequisites

- Node 20+
- JDK 17, Android SDK (API 34), Android Studio
- A physical Android device (recommended) — GPU inference needs OpenCL; emulators rarely have it
- ~3 GB free space on the device for the model

## 1. Install

```powershell
npm install
```

## 2. Generate the Android project

Expo Go cannot load native modules; use a dev client.

```powershell
npx expo prebuild --platform android --clean
```

## 3. Get the Gemma 4 E2B `.litertlm` file

Go to the gated Hugging Face repo, sign in, accept the Gemma license, and download from the **Files** tab:

- https://huggingface.co/google/gemma-4-E2B-it-litert-lm-preview

You'll get a `.litertlm` file. Rename it to `gemma-4-e2b-it.litertlm` to match `MODEL_FILENAME` in [App.tsx](App.tsx) — or change that constant to whatever the file is called.

> Other `.litertlm` models are listed at [Hugging Face › LiteRT Community](https://huggingface.co/litert-community). To validate the pipeline before you have access, `Gemma3-1B-IT.litertlm` from that org works as a drop-in.

## 4. Push the model onto the device

```powershell
# Build + install once so the app's documents dir exists
npx expo run:android

# Then push the model (replace the package id if you changed it in app.json)
adb push .\gemma-4-e2b-it.litertlm `
  /sdcard/Android/data/com.example.gemmachatbot/files/gemma-4-e2b-it.litertlm
```

If the app reports "Model not found", the banner shows the exact expected path — copy it and adjust the `adb push` destination.

Alternative for development: place the file under `/sdcard/Download/` and change `MODEL_PATH` in [App.tsx](App.tsx) to `'/sdcard/Download/gemma-4-e2b-it.litertlm'`.

## 5. Run

```powershell
npx expo run:android
```

In the app:

1. Tap **Load model** — first load can take up to ~10 s (per the LiteRT-LM docs); it runs on a background coroutine.
2. Type a prompt and hit **Send**. Tokens stream from `Conversation.sendMessageAsync`'s `Flow<Message>` and surface as `onPartialResult` events on the JS side.

## Tuning

In [App.tsx](App.tsx), `GemmaLlm.load({...})` accepts:

| option | meaning |
| --- | --- |
| `modelPath` | absolute path to the `.litertlm` bundle |
| `backend` | `0` = CPU, `1` = GPU (default), `2` = NPU |
| `systemInstruction` | system prompt baked into the `Conversation` |
| `topK`, `topP`, `temperature` | sampling (Gemma 4 defaults: `64 / 0.95 / 1.0`) |

NPU requires bundled vendor libs — see the [LiteRT-LM NPU guide](https://ai.google.dev/edge/litert/lm/android).

## Troubleshooting

- **Cannot resolve `com.google.ai.edge.litertlm:litertlm-android`** — the AAR is on Google Maven, which Expo's prebuild already includes. If you customized `android/build.gradle`, ensure `google()` is in `repositories`.
- **Crash on `engine.initialize()`** — usually a corrupted/incompatible `.litertlm` file or an x86 emulator. Use a real arm64 device.
- **OOM** — pick a smaller-quant `.litertlm` variant or fall back to CPU backend.
- **GPU backend fails to load OpenCL** — make sure the `<uses-native-library>` entries from [AndroidManifest.xml](modules/gemma-llm/android/src/main/AndroidManifest.xml) are merged into the final app manifest (they should be by default).
