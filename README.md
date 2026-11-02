# AETHER

A personal Life OS and exam-prep companion, built offline-first and
privacy-first: your data stays on the device unless you explicitly use one
of the few opt-in network actions listed below.

## What it does

- **Life OS:** goal hierarchy (Life Vision down to tasks), daily check-ins,
  Life Score, journal, gym tracking, research notes, an editable weekly
  timetable with real alarms.
- **Personalization (on-device):** pick a persona (Student, Researcher,
  Athlete, Professional) as a starting point, then override tone, planning
  style, chronotype, waking hours and modules. AETHER also learns from your
  own behavior (when you finish tasks, which domains you follow through on,
  which suggestions you accept) with a visible confidence level and a
  one-tap "forget".
- **Study Hub (NotebookLM-style):** notebooks of your own sources (pasted
  text, .txt/.md, selectable-text PDFs, web pages). Cited answers grounded in
  those sources, interactive digests, flashcards with spaced repetition,
  quizzes, GATE-style MCQ/MSQ/NAT practice steered by your own description
  of the exam pattern, and mind maps. An optional on-device language model
  (MediaPipe + a Gemma `.task` file you provide) phrases answers; without it
  everything falls back to extractive, citation-backed answers.
- **Life Map:** your goal hierarchy as a pan/zoom mind map.
- **Glass UI:** liquid-glass navigation (refraction, spring physics, haptics)
  built on [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass).
  Refraction needs Android 13+; older devices get blur only.

## Network use

Everything runs on-device except three opt-in actions:

1. **Web reader** fetches one public page you ask it to (Study Hub).
2. **AI schedule generation** with your own Groq key (Goals). This is the one
   remaining cloud dependency and a candidate for replacement by the
   on-device model.
3. Nothing else. No analytics, no accounts, no sync.

## Architecture

- `shared/` Kotlin Multiplatform core: SQLDelight store, and all engines
  (scoring, context, personalization, study, digest, exam, mind map). No UI.
- `androidApp/` Jetpack Compose shell.

Engine unit tests live in `shared/src/commonTest`.

## Building

CI (`.github/workflows/android-build.yml`) builds the debug APK on every push
to `main` and attaches it to `v*` tags. Locally: JDK 17, Android SDK 35,
Gradle 8.9:

```
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle :shared:testDebugUnitTest
gradle :androidApp:assembleDebug
```

## Known limitations

- Database is plain SQLite in app-private storage (not encrypted at rest).
- Scanned PDFs need OCR, which isn't implemented. JavaScript-rendered or
  login-gated pages can't be read by the web reader.
- Small on-device models make mistakes, especially on numerical questions;
  generated practice content is labelled and should be verified.
- Extractive answers are keyword/BM25 based and English-oriented.
- The model file is not downloaded by the app; you pick it once.
- No iOS/Desktop targets yet.

## Versioning

Semantic versioning in `androidApp/build.gradle.kts`, mirrored as git tags
and GitHub Releases. See `CHANGELOG.md`.
