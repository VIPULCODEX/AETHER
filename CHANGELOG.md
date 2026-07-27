# Changelog

All notable changes to AETHER are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versioning follows
[Semantic Versioning](https://semver.org/).

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
