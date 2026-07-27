# Changelog

All notable changes to AETHER are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versioning follows
[Semantic Versioning](https://semver.org/).

## [0.1.1] - 2026-07-27

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
