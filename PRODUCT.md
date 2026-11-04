# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Adults who choose to limit their own short-video and feed scrolling. They install Feedcut on their own phone, can switch it off at any time, and use it as a habit aid, not as parental control or a lock. The first user is the maintainer, on a Xiaomi 17 with HyperOS.

## Product Purpose

Feedcut closes Instagram Reels, YouTube Shorts and TikTok the moment they open, and keeps Facebook on Marketplace and Groups by switching every other tab to Marketplace. Success: the user keeps the useful parts of these apps and stops losing time to the feeds, without having to think about it.

## Positioning

The only blocker in its open-source neighbourhood that keeps Facebook useful (Marketplace, Groups) while removing the feed, and it does this with no internet permission and without reading screen text, except the bottom tab bar's labels. Rules are a readable JSON file that ships with each version and merges into the user's own settings on update. The app warns when a rule may have broken after an app update.

## Operating Context

- The main screen is opened rarely. The user glances at it: is it working, how many screens did it close today, how much time that saved. They rarely change per-app settings, and almost never touch the rules file.
- Setup happens once and is the hard part: accessibility service (with Android 13+ restricted settings), and on Xiaomi/HyperOS a battery setting (No restrictions) and Autostart, without which the system freezes the service.
- The phone may be in light or dark mode. The maintainer uses dark mode.

## Capabilities and Constraints

- Native Android, Kotlin, XML views and Material Components. No Compose. minSdk 26, targetSdk 36.
- No `INTERNET` permission, no analytics, no account. Everything stays on the phone; backup is off.
- Data on hand: screens closed today (counter), per-app last block time and app version. A daily history and total are not stored yet.
- Time saved cannot be measured. Any "time saved" figure is an estimate from closed screens and must show its assumption.
- English only. The app name is Feedcut; package `io.github.yxonyx.feedcut`.

## Brand Commitments

- Name: Feedcut.
- Voice: a strict coach. Direct and firm, states what the feeds cost, does not praise or shame. No emoji, no hype.

## Evidence on Hand

- Real counts from the maintainer's phone (for example 42 screens closed in one day). No users, reviews, download numbers or press yet; none may be invented.

## Product Principles

1. Working is the only status that matters: make "on, off, or stuck" unmistakable.
2. Setup is the product's hardest moment; guide it step by step and say when something is wrong.
3. Honest numbers: counts are real, estimates say they are estimates.
4. Keep developer controls (screen markers, rules file) out of the everyday view.
5. Nothing leaves the phone.

## Accessibility & Inclusion

Readable in light and dark mode with sufficient contrast; status must not rely on color alone. Respect system font scaling.
