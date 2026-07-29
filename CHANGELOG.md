# Changelog

All notable changes to AETHER are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versioning follows
[Semantic Versioning](https://semver.org/).

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
