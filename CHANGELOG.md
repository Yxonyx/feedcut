# Changelog

All notable changes to Feedcut. Versions follow [Semantic Versioning](https://semver.org); the rules file has its own `version` number, listed with each release.

## Unreleased

### Fixed
- A screen that was still opening could be missed when the app sent no more events, for example on Android 8. After a window change that showed nothing, Feedcut now checks again twice on its own.
- The "Allow restricted settings" hint only shows on Android 13 and later, where the setting exists.

### Added
- Daily limits. Tap an app on the main screen and choose how many minutes a day its screen may stay open (`dailyMinutes` in the rules file). Feedcut warns when one minute is left, then cuts the screen for the rest of the day. Without a limit it cuts at once, as before.
- A rule can no longer block Feedcut itself, Settings, the system UI or the package installer, so the user can always switch Feedcut off.
- A setup step for OPPO, realme, OnePlus, vivo, iQOO, Huawei and Honor, which have a background switch of their own.

### Changed
- License: Apache-2.0 instead of GPL-3.0. Version 1.1.0 stays available under GPL-3.0.

## 1.1.0 (versionCode 2), rules version 4

First public release.

### Added
- Leaves Instagram Reels, YouTube Shorts (also YouTube ReVanced) and the TikTok video feed.
- Keeps Facebook to Marketplace and Groups by tapping the Marketplace tab when another tab opens (`allowTabs`).
- Main screen: on, off or stuck; screens cut today with an estimate of the time kept; the last 7 days; per-app state.
- Rule health: the main screen says when an app updated after the last cut, so the rule may be broken.
- Setup steps for the accessibility service, battery and (Xiaomi) Autostart, with direct links to each settings screen.
- Advanced screen: per-app Back or Home, screen markers, the raw rules file with validation, restore defaults.
- Rules updates merge into the user's file (three-way merge), so app updates keep user edits.

### Privacy
- No `INTERNET` permission, no analytics, backup off.
