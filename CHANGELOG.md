# Changelog

All notable changes to AETHER are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versioning follows
[Semantic Versioning](https://semver.org/).

## [0.16.0] - 2026-11-02

### Added
- Study Hub: video as a source, with a built-in player. Pick your own video
  file and its subtitle file (.srt or .vtt). The subtitles become the source,
  so summaries, cited answers, flashcards, practice and mind maps all work on
  what is said in the video. "Play video" opens the player (Media3) with a
  live transcript: the spoken line is highlighted and tapping any line jumps
  the video there. Files stay where they are; nothing is uploaded. (Videos
  without subtitles need on-device speech-to-text, which isn't built yet.)
- Learning from study: reviewing flashcards and answering quizzes now tells
  AETHER *when* you study (only the hour, no content), so it can find your
  study hours and use them for peak-window suggestions and timetable planning.
- Database schema v9 (migration 8.sqm): video URI and timed transcript.

## [0.15.1] - 2026-11-02

### Fixed
- "What AETHER has learned" stayed at 0 observations: it only learned from
  finished goal-tasks and suggestion responses, while daily use happens through
  routines. Routine ticks now carry a timestamp (migration 7.sqm) and feed the
  learner: peak hours, follow-through per domain (done vs scheduled over the
  last 14 days), and the observation count. The Personalize screen shows
  exactly where observations come from and what's needed for the first insight.

### Added
- Home: log today's energy (1-5) and last night's sleep. Energy drives what the
  Context Engine suggests right now, and energy + sleep together let AETHER
  learn how rest affects you.
- Suggestions now include today's still-open routines ("Do: GATE revision.
  Daily routine, still open today, your peak window"). One tap on the card
  ticks the routine; "Not now" moves to the next best item.

## [0.15.0] - 2026-11-02

### Added
- One-tap on-device AI: "Download AI model" in the Study Hub fetches a small
  built-in default (Qwen2.5-0.5B-Instruct, Apache-2.0, ungated, about 547 MB)
  once, with a progress bar and resume after a dropped connection. No file to
  find, no account. After that everything runs offline. "Use my own file"
  remains for advanced users (e.g. Gemma 3 1B).
- Each model family gets its own chat template (Qwen and Gemma), chosen
  automatically; prompts are sized to the default model's 1280-token window.

## [0.14.1] - 2026-11-02

### Fixed
- AI timetable failed with `404 model_not_found`: Groq retired the hard-coded
  model. The client now asks Groq which models the user's own key can use and
  picks the best chat model (skipping speech/guard models), retries once if a
  model disappears, and tolerates reasoning-model replies (code fences,
  `<think>` blocks). 401 and 429 errors now say what to do.
- Pink smudge under the status bar: the ambient glow was being over-saturated
  by the top bar's vibrancy; the bar is now blur-only and the glow is softer
  and lower.

## [0.14.0] - 2026-11-02

### Added
- Routines (More -> Routines): your daily work and fixed commitments. A
  routine has a duration, days of the week and an optional fixed start time
  (college, office). "Rebuild my timetable" plans the whole week on-device:
  fixed blocks stay where they are, the rest is fitted into your waking hours,
  long demanding routines go into your peak-focus window (learned from your
  behavior, or the chronotype you set), and anything that doesn't fit is
  reported instead of dropped. Adding or removing a routine re-plans
  automatically. Alarms follow the timetable; fixed commitments stay silent.
- Home: a "Today" card lists today's routines with one-tap ticks and shows
  your streak (current, best, days this week). A day counts if you tick a
  routine, finish a task or complete the mission; today not being done yet
  doesn't break the streak until the day ends. Ticking every routine also
  marks today's mission done.
- Database schema v7 (migration 6.sqm): `Routine`, `RoutineLog`. Removing a
  routine archives it so past completions still count toward streaks.

### Changed
- Theme: an indigo wash with two soft colour glows now sits behind every
  screen (and inside the recorded glass layer, so the nav and top bar have
  something to refract), cards have lit-from-the-corner gradients and a
  hairline edge.
- Text no longer wraps or overruns in tight places: chips, buttons, nav labels
  and the top bar are single-line with ellipsis; Home module rows are clamped.

## [0.13.1] - 2026-11-02

### Fixed
- Nav bar: the selected tab's icon and label were blue on a blue pill and
  disappeared; the active tab is now white on a pale frosted pill. In a
  device screenshot the pill also showed content from the wrong part of the
  screen ("Ge" from the page title, a ghost "Home" pill near the top). Going
  back to the pill structure that rendered correctly in 0.7.x (uniform scale
  applied outside the glass layer, no squash/stretch, no depth effect or
  custom inner shadow) and dropping the vertical slide on screen changes,
  which moved the recorded glass layer. Slide is now critically damped:
  smooth glide with no wobble.

### Added
- Settings -> "Test notification and alarm": show the branded notification
  immediately, or ring the full-screen alarm after 15 seconds.

## [0.13.0] - 2026-11-02

### Changed
- Glass navigation rebuilt around spring physics. The pill is now a spring
  that chases your finger (trailing slightly) and overshoots a touch when it
  settles, squashes and stretches with its speed, lights up while held, and
  gives haptic detents as it crosses tabs. Refraction is deeper
  (`depthEffect`), with a rim light, drop shadow and inner glow on both the
  bar and the pill; icons spring and recolor as tabs change.
- Top bar: stronger blur, a vertical tint gradient and a single hairline
  instead of the generic rim and shadow.
- Cards sink slightly under the finger and spring back; screens fade and
  rise in on navigation.
- Alarm screen: a slow breathing glow behind the activity name.

### Added
- AETHER's own sounds (generated by `tools/make_sounds.py`, tweakable): a
  three-note glass-bell chime for notifications and a swelling arpeggio loop
  for the alarm screen. The system ringtone is only a fallback.
- Branded alarm notification: accent colour, status-bar glyph, expanded
  text, Snooze and Dismiss buttons that work without opening the app, a
  matching vibration pattern and light colour. (New channel id so the sound
  applies; the old channel is removed.)
- App icon (adaptive, with a monochrome variant for themed icons).
- Unit tests for the shared engines (`shared/src/commonTest`, 27 tests):
  personalization, context engine, grounded answers, SM-2, digest, GATE
  question parsing, mind-map layout. CI runs them before the APK build.

### Fixed (pre-release review)
- Grounded answers: the citation now points at the sentence that actually
  supports the answer (small models mis-number their [n]), and the best
  passage is shown together with the chunk that follows it, so evidence
  split across a chunk boundary isn't lost.
- Web reader: `http://` links are upgraded to `https://` (Android blocks
  cleartext traffic for modern apps); a share without a link now says so.
- On-device model is released from RAM when the app is backgrounded under
  memory pressure and reloads on demand, instead of staying resident.
- Practice questions and explanations are labelled as AI-written, with a
  reminder to verify numerical answers.
- SQL: `selectCardCounts` was declared before the table it queries.

## [0.12.0] - 2026-11-02

### Added
- Study Hub: exam-aware learning. Describe how an exam asks about a subject
  ("GATE CS: mostly numericals on cache mapping...") and the on-device model
  uses it to explain material and to write GATE-style practice questions
  (MCQ, MSQ all-or-nothing, and NAT with rounding tolerance) from your own
  sources, with answers and explanations.
- Web page reader: paste a link, or use Share -> AETHER in your browser. The
  page is fetched once, reduced to readable text (nav, ads and comments
  removed), saved as a source, and opened as an interactive Digest: TL;DR,
  outline, expandable key points ("Explain simply" via the model), and
  tappable key terms. Public pages only; no logins or paywall bypass.
- Storage overview: size of each notebook, the on-device model and free space,
  with one-tap delete for any of them.
- Database schema v6 (migration 5.sqm): notebook exam notes, source URLs.

### Changed
- The Summary tab became Digest.

## [0.11.0] - 2026-11-02

### Added
- Study Hub: optional on-device language model (MediaPipe, Gemma `.task`
  file you pick once; nothing is downloaded or uploaded). When loaded,
  answers and summaries are written by the model but stay grounded: if
  retrieval finds nothing in your sources the model isn't called, it must
  cite passages as [n], and any failure falls back to the extractive answer.
- Study Hub: PDF import (selectable-text PDFs, extracted on-device via
  PdfBox-Android). Scanned PDFs are reported as having no text.

## [0.10.0] - 2026-11-02

### Added
- Mind maps, computed on-device and always live (derived from your data, so
  they never go stale). Study Hub notebooks get a Map tab: sources become
  topic branches with their key sentences as leaves. More -> Life Map shows
  your Life Vision -> goals -> tasks hierarchy, with finished tasks in green.
- Pan and pinch-zoom canvas; tap a node for its full text, tap again to
  collapse or expand a branch. Radial layout widens its rings so crowded
  maps don't overlap.

## [0.9.0] - 2026-11-02

### Added
- Study Hub (More -> Study Hub), NotebookLM-style and fully on-device.
  Notebooks hold your own sources (pasted text or imported .txt/.md).
  Ask questions and get answers assembled only from your sources, each with
  a citation (BM25 retrieval); "not found" is said plainly instead of
  guessing. Also: extractive summaries, auto-generated flashcards
  (definition and cloze) scheduled with SM-2 spaced repetition, and
  multiple-choice quizzes built from the cards.
- Database schema v5: `Notebook`, `StudySource`, `Flashcard` (migration 4.sqm).

## [0.8.0] - 2026-11-02

### Added
- Personalization, fully on-device. A new Personalize screen (More ->
  Personalize) lets each user pick a starting persona (Student, Researcher,
  Athlete, Professional, Custom) and then override everything it sets: coach
  tone, planning style, chronotype, waking hours, and which modules (Gym,
  Research, Journal) show up.
- AETHER now learns from your own history: when you actually finish tasks
  (peak focus window and chronotype), which domains you follow through on,
  how sleep relates to energy, and which suggestions you accept or skip.
  Confidence grows with data and is shown openly; "Forget what you've
  learned" clears it, and learning can be switched off entirely.
- Dashboard: "Not now" on the mission card records the rejection and
  surfaces the next-best item; finishing the mission counts as acceptance.
  A "Make AETHER yours" card prompts first-time setup.

### Changed
- Context Engine ("what should I do now?") now ranks real open tasks and
  goals by deadline pressure, impact vs effort, today's energy, your peak
  window, persona and learned affinity, and explains the top reasons in
  your chosen tone. Disabled modules never appear in suggestions.
- Database schema v4: `Preference` and `BehaviorEvent` tables (migration 3.sqm).

## [0.7.5] - 2026-07-29

### Fixed
- Nav pill's drag no longer hops between tabs: it was quantized to whole
  tab-width jumps even while actively held, so sliding your finger across
  the bar looked like discrete steps. It now tracks the raw finger
  position pixel-for-pixel while dragging, and only snaps to the nearest
  tab once you let go.

## [0.7.4] - 2026-07-29

### Changed
- Nav pill lifts (scales up slightly, rises off the bar) the instant it's
  grabbed, then eases back down smoothly on release, instead of staying
  flat the whole time it's being dragged.

## [0.7.3] - 2026-07-29

### Changed
- Bottom nav's glass pill is now draggable: press and hold anywhere on the
  bar and it follows your finger live across tabs instead of only jumping
  between them on tap. Releasing commits navigation to whichever tab it
  lands on; a plain tap still works the same as before since it's just a
  zero-distance drag.

## [0.7.2] - 2026-07-29

### Fixed
- Bottom nav's selected-tab indicator now actually slides between tabs.
  Every screen instantiated its own glass scaffold (backdrop + bottom bar)
  inside its own NavHost destination, so switching tabs tore down and
  rebuilt the bar's remembered animation state before it could animate —
  the pill just appeared already at its target position. Split the chrome
  into `AetherAppChrome` (owns the shared backdrop and the single
  persistent bottom nav bar, wraps the NavHost once) and a slimmer
  per-screen `AetherScaffold` (just the title bar + content), so the
  indicator's state — and its slide — now survives navigation.

## [0.7.1] - 2026-07-29

### Fixed
- Top glass bar no longer overlaps content on devices with a taller status
  bar/notch or larger font scale: content top padding was a fixed 96.dp
  guess that could be shorter than the bar's real rendered height. It's now
  measured live (`onSizeChanged`) and threaded down through a
  `CompositionLocal`, so `AetherBars.TopContentPadding` always matches the
  actual bar.
- Bottom nav's selected-tab indicator now glides smoothly between tabs
  (420ms `FastOutSlowInEasing` tween) instead of popping with a slight
  spring overshoot.
- CI: pinned `backdrop` to a Kotlin/Compose-compatible version, bumped
  Kotlin to 2.3.21 and the Compose BOM to 2026.06.01 to match, migrated
  off the now-hard-error `kotlinOptions { jvmTarget }` DSL, and fixed a
  missing `getValue` import that only surfaced once the above were fixed.

## [0.7.0] - 2026-07-28

### Changed
- Navigation chrome rebuilt around a real liquid-glass effect
  ([Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)):
  the bottom tab bar and top title bar are now floating, translucent panels
  that live-sample, blur, and refract whatever content is actually scrolling
  underneath them via a shared `LayerBackdrop` — not a tinted rectangle drawn
  over an opaque `Scaffold` bar, which is what every earlier version had.
  Required moving off Material3 `Scaffold`'s `topBar`/`bottomBar` slots (they
  reserve their own space and never let content render behind them) to an
  edge-to-edge `Box` where every screen's own `LazyColumn` now carries
  top/bottom `contentPadding` (`AetherBars.TopContentPadding` /
  `BottomContentPadding`) instead of relying on Scaffold-computed insets.
  The bottom bar keeps a capsule "pill" indicator that spring-animates to the
  selected tab and gets its own nested glass/refraction pass.
- Bottom tab icons replaced: the five emoji (🏠🎯💪📔⋯) are gone, swapped for
  proper outlined/filled Material icons (`material-icons-extended`) that
  swap weight on selection — emoji rendering is inconsistent across devices
  and read as a placeholder rather than a deliberate icon set, especially
  now that they'd be sitting on a glass surface instead of a flat one.
- On API < 31 (no `RenderEffect`) the library degrades gracefully — bars stay
  translucent and tinted but without live blur/refraction — rather than
  crashing or needing a separate code path.

## [0.6.0] - 2026-07-27

### Added
- Real wake-up alarms for the weekly timetable — off by default, opt in from
  Settings. Rings and vibrates over the lock screen like the stock Clock
  app (`AlarmActivity`), scheduled via exact `AlarmManager` alarms
  (`AlarmScheduler`), delivered through a full-screen notification
  (`AlarmReceiver`, the sanctioned pattern for launching an Activity from a
  background broadcast on modern Android), and restored after a reboot
  (`BootReceiver`). Settings walks through both required permissions
  (notifications, and "Alarms & reminders" on Android 12+) instead of the
  app silently failing to ring.
- Attachments: Journal entries and Research notes can now attach a photo or
  PDF via the system file picker, with persisted read permission so the
  link survives app restarts — nothing is copied into app storage. Gym
  gained an entirely new Progress section (`BodyLog` table): log weight
  and/or a photo check-in and see a timeline with a weight-trend line,
  which the module had zero support for before this (previously just a
  static profile form).
- Sets × reps added to every exercise in the 4-day split — previously only
  posture cues existed with no actual training prescription.

### Changed
- Full visual hierarchy pass: introduced `AetherCard` (neutral surface tile)
  as the default for lists and everyday content — goal rows, schedule
  slots, journal entries, research notes, body-log rows — reserving the
  saturated `AccentCard` gradient for a handful of true hero moments (Life
  Score, Life Vision, Today's Mission, Nutrition plan). Using the loud
  accent card for literally everything was the main reason the app read as
  messy/amateur rather than hierarchical. Added matching `SectionHeader`,
  `EmptyState`, and themed text field/button colors so forms stop looking
  like raw Material3 defaults clashing against the dark theme.
- Dashboard: time-of-day greeting instead of a static header, a
  week-over-week trend line on the Life Score card (identity-over-streaks
  framing, e.g. "More consistent than last week"), a progress bar on the
  Life Vision card, and module rows that actually surface real state
  (Gym's last check-in weight, Research's latest note) instead of just
  linking out blind.
- Goals: breadcrumb rebuilt as tappable pill chips instead of tiny inline
  text; goal cards now show a real progress bar, not just a percentage
  string.

## [0.5.1] - 2026-07-27

### Added
- Hierarchical Goal System (Milestone 1 of the AI-first Higher Study OS
  roadmap): Goals now form a tree — Life Vision → Long Term → Domain →
  Quarterly → Monthly → Weekly — via a new `goalType` + self-referencing
  `parentGoalId` on `Goal`. Goals screen replaced its flat list with a
  breadcrumb-navigable tree view (still built entirely from the existing
  `AccentCard`/`FilterChip` primitives, no new visual style introduced).
- Task table ("Today's Actions"): each Task belongs to a Goal, so every
  action always knows which goal — and by walking `parentGoalId` up the
  tree, which Life Vision — it serves. Manage them from the Goals screen
  under whichever goal is selected.
- Dashboard gained a "Life Vision" card next to the existing Life Score
  card, showing roll-up progress toward the nearest Life Vision goal (e.g.
  "43% closer to: Become a Research Scientist") — computed by
  `ScoringEngine.computeGoalTreeProgress`, which averages child-goal
  progress recursively down to task completion ratios. No black-box
  numbers, same principle the rest of the engine already follows.
- SQLDelight schema migrations (`1.sqm`) introduced for the first time —
  existing installs upgrade in place (pre-existing goals become root-level
  Weekly goals) instead of the schema being hand-edited going forward.

### Changed
- Split `AetherRepository` into `AetherRepository` (Journal/CheckIns/Focus
  Areas/Schedule/Profile/Research) and a new `GoalsRepository` (Goals +
  Tasks), since the hierarchy work was the point past which one repository
  class stops being manageable.

## [0.5.0] - 2026-07-27

### Fixed
- Status bar overlap: the old hamburger drawer icon collided with the phone's
  status bar because `enableEdgeToEdge()` was on but nothing accounted for
  system-bar insets. Replaced entirely (see below), so this class of bug
  goes away rather than being patched.

### Changed
- Navigation rebuilt as a bottom tab bar (Home/Goals/Gym/Journal/More) using
  Material3 `Scaffold`, which handles system-bar insets correctly — more
  native to the iOS-leaning direction than a Material drawer ever was, and
  fixes the overlap bug structurally instead of patching padding onto the
  old drawer.
- Full visual pass toward an iOS/ColorOS/OxygenOS blend: cards (`AccentCard`,
  replacing `BlockCard`) are now solid, deep-toned tiles with a subtle
  vertical gradient and white text, no thick border — softer and more
  "premium app" than the earlier flat poster-block look.
- Dashboard's Life Score card no longer shows raw
  "Consistency X · Execution Y · Goals Z%" numbers — replaced with a 7-dot
  weekly strip showing which of the last 7 days the mission was completed,
  a more legible, useful "how's this week going" glance.
- Gym's 4-day split corrected from the earlier bodybuilding-style split to
  Push / Pull / Legs / Full Body, with updated exercises and posture cues.
- Journal: added mood chips (😊😐😔 etc., tap to tag an entry) and an
  "entries this month" line, so the screen isn't just a bare text box.

### Added
- Research module (previously a placeholder): gated behind "Research" being
  a selected focus area, same pattern as Gym. Log papers/ideas with a
  status (Idea/Reading/Read/Writing) and a note.
- More screen: houses Research, GATE Prep (still not built), and Settings,
  reached via the bottom bar's "More" tab.

## [0.4.1] - 2026-07-27

### Fixed
- Build-breaking compile error introduced in 0.3.0: `FocusArea` has a
  single column (`name`), so SQLDelight returns those query rows as a
  plain `String` directly rather than a wrapper object — `rows.map { it.name }`
  in `AetherRepository.observeFocusAreas()` tried to call `.name` on a
  `String`, which doesn't exist. This was silently breaking both the
  0.3.0 and 0.4.0 CI builds. Fix: return the mapped list directly.

## [0.4.0] - 2026-07-27

### Added
- AI-generated scheduling via Groq (Llama 3.3 70B), bring-your-own-key:
  a new Settings screen lets each user paste their own free Groq API key
  (from console.groq.com/keys). Goals screen now has a free-text box
  ("describe your routine and constraints") and a "Generate with AI"
  button that calls Groq directly from the device using that key. Only
  the focus areas and the typed description are sent — no other app data.
  Errors from the API (bad key, rate limit, etc.) surface directly in the
  UI instead of failing silently.
- `INTERNET` permission added (required for the above; this is the app's
  first and only network-dependent feature, and it's fully opt-in).

### Changed
- Dashboard no longer lists every active goal inline — that list had no
  bound and would grow messy as goals were added. The Goals summary card
  (with a count) is enough on the dashboard; the full list lives on the
  Goals screen where it belongs.

## [0.3.0] - 2026-07-27

### Added
- Goals screen now leads with a multi-select "focus area" chip row (Gym,
  Research, GATE, JEE, College Work). Selecting areas generates a basic
  weekly timetable (`BasicScheduleGenerator`) distributing time blocks
  round-robin across them — every slot is tap-to-edit. This is a
  deterministic v1; an LLM-generated version (via API call, see below)
  is the planned upgrade path, same data shape so it's a drop-in swap.
- Gym module (previously a placeholder): gated behind "Gym" being a
  selected focus area. Includes a profile form (height/weight/age/sex/
  activity level/goal), a BMR/TDEE calculator (Mifflin-St Jeor) giving
  target calories and protein intake, and a curated 4-day split (Chest/
  Triceps, Back/Biceps, Legs, Shoulders/Abs) with text-based posture cues
  per exercise. No photos/videos yet — flagged as a follow-up needing
  either bundled licensed images or a verified exercise-database API.

### Decided
- AI-generated scheduling will use an API call (Claude API) rather than
  an on-device model — on-device models aren't reliable enough for
  multi-constraint natural-language schedule generation, and calls are
  infrequent enough that cost is negligible. Only goal selections and
  the free-text schedule description will be sent; the rest of the Life
  Data Store never leaves the device. Not yet wired up — needs an
  Anthropic API key before the network call can be built.

## [0.2.0] - 2026-07-27

### Added
- Real navigation: hamburger menu + drawer (`AetherScaffold`) listing every
  module — Dashboard, Journal, Goals, and Gym/Research OS/GATE Prep as
  "coming soon" placeholders. Previously there was no way to reach anything
  besides Dashboard and Journal, and no indication other screens existed.
- Goals screen: add a goal (title + domain) and see the active list —
  closes the "no goal-adding UI" gap called out in earlier versions.
- Today's Mission card is now actually functional: tapping it marks (or
  unmarks) the mission done for today, which is what actually feeds the
  Execution Score — previously the score was always 0 because nothing
  wrote to daily check-ins.
- ComingSoon screen for unbuilt modules, so drawer items never lead nowhere.

### Changed
- Full visual redesign: replaced the translucent monochrome glass system
  with a bold, flat, colorful "blocky retro" style (`BlockCard`) — solid
  color fills, thick cream outlines, one color per module (amber/Life
  Score, coral-or-teal/Mission, magenta/Goals, teal/Journal). Solid color
  blocks also read as tappable far more clearly than the old glass cards
  did, which was part of why the app felt unresponsive.

## [0.1.2] - 2026-07-27

### Fixed
- Critical contrast bug: Dashboard and Journal screens were missing a `Surface`
  wrapper, so any `Text` without an explicit color fell back to Compose's
  default black text color — rendering headlines, "Today's Mission", "Journal",
  and goal titles as near-invisible black-on-near-black. Wrapping both screens
  in a `Surface(color = MaterialTheme.colorScheme.background)` fixes the
  default text color for all current and future text on these screens.

## [0.1.0] - 2026-07-27

### Added
- Initial project scaffold: Kotlin Multiplatform shared core (`shared`) +
  native Android Compose UI (`androidApp`)
- Local-only data layer (SQLDelight): Journal entries, Goals, Daily check-ins
- Scoring Engine v1: Life Score, Consistency Score, Execution Score, Goal Completion %
- Context Engine v1: "what should I do now?" suggestion logic, always reasoned
- Dashboard screen: Life Score, Today's Mission, active goals list
- Journal screen: write and view entries
- Dark, minimal, glass-surfaced design system
