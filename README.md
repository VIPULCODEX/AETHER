# AETHER

A personal Life OS — not a habit tracker. Everything in this app answers one
question: *does this move me closer to my life's vision?* Built offline-first
and privacy-first: your data never leaves the device unless you explicitly
opt into a specific integration.

## Architecture

- **`shared/`** — Kotlin Multiplatform core. All business logic lives here:
  the local Life Data Store (SQLDelight), the Scoring Engine (Life Score,
  Consistency, Execution, Goal Completion), and the Context Engine
  ("what should I do now?"). This module has no UI — every platform
  (Android today, iOS/Desktop/Web later) is a thin shell on top of it.
- **`androidApp/`** — native Android app using Jetpack Compose, the current
  and only UI shell. Design system: dark-mode-only, glass-surfaced,
  monochrome with a single restrained accent color reserved for
  positive-trajectory signals.

Package names: `com.aether.core.*` (shared), `com.aether.android.*` (Android UI).

## What's implemented (v0.1.0)

- Local persistence: Journal entries, Goals, Daily check-ins
- Scoring Engine v1: Life Score = average of Consistency Score (rolling
  14-day engagement), Execution Score (rolling 7-day plan-vs-actual), and
  Goal Completion %
- Context Engine v1: time + energy + nearest-deadline-goal based suggestion,
  always returned with an explicit reason (never generic)
- Dashboard screen: Life Score, Today's Mission, active goals
- Journal screen: write and view entries, fully persisted locally

## Known limitations (not yet built)

- No Gym / Research OS / GATE prep / Analytics-full modules yet — those
  follow the same shared-core + thin-UI pattern established here
- No "Add Goal" or daily check-in UI yet (repository methods exist, not wired up)
- Database is plain local SQLite (app-private storage) — not yet encrypted at rest
- No app launcher icon yet
- Glassmorphism is approximated (translucent surface + hairline border);
  true backdrop blur (API 31+ RenderEffect) is a follow-up
- No iOS/Desktop/Web targets configured yet — the shared module's
  commonMain/androidMain split makes adding them straightforward later
- No automated tests yet

## Building

This machine doesn't have a JDK, Android SDK, or Gradle installed, so this
project has not been compiled or run yet. To build it:

1. Install **Android Studio** (Ladybug 2024.2+ or newer — needs Kotlin 2.1 support)
2. Open this folder as a project — Android Studio will use its bundled JDK
   and Gradle, and will regenerate the Gradle wrapper jar automatically on
   first sync (only `gradle-wrapper.properties` is checked in; the wrapper
   jar binary is not, since it can't be produced without a JDK/Gradle here)
3. Run on a device or emulator, API 26+

## Versioning & releases

Semantic versioning (`MAJOR.MINOR.PATCH`), tracked in
`androidApp/build.gradle.kts` (`versionName`/`versionCode`) and mirrored as
git tags (`v0.1.0`, `v0.2.0`, ...). Each tag corresponds to a GitHub Release
with build artifacts attached once CI/build signing is set up. An iOS target
will adopt the same version number once it exists, so Android and iOS stay
in lockstep release-to-release.

Current version: **0.1.0** (initial architecture + Dashboard + Journal).
