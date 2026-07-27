# Changelog

All notable changes to AETHER are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versioning follows
[Semantic Versioning](https://semver.org/).

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
