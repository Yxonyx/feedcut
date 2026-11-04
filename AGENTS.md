# AGENTS.md

Notes for coding agents (Claude Code, Codex, Cursor and others) working on Feedcut. People can read it too.

## What this is

Feedcut is an Android accessibility service that leaves short-video screens (Reels, Shorts, TikTok) and keeps Facebook on Marketplace. Plain Kotlin and XML views, no Compose, no network code.

| File | Role |
| --- | --- |
| `app/src/main/assets/rules.json` | Default rules. Most fixes are a change here. |
| `app/src/main/kotlin/io/github/yxonyx/feedcut/Rules.kt` | Pure decision logic: hint matching, tab redirect, rule merge on update. Unit tested. |
| `app/src/main/kotlin/io/github/yxonyx/feedcut/GateService.kt` | The accessibility service. Reads the window, calls `Rules.kt`, presses Back/Home or taps a tab. |
| `app/src/main/kotlin/io/github/yxonyx/feedcut/RuleStore.kt` | Loads, validates, saves (atomically) and merges the rules file on the phone. |
| `app/src/main/kotlin/io/github/yxonyx/feedcut/CheckTimer.kt` | When to check: coalescing events, the debounce after an action. Pure, unit tested. |
| `app/src/main/kotlin/io/github/yxonyx/feedcut/BlockLog.kt` | Last block time and app version per package, for the "rule may be broken" hint. |
| `app/src/test/kotlin/io/github/yxonyx/feedcut/` | Unit tests: rules, timer, rules file parsing (with the real org.json). |

## Hard rules

- **No `INTERNET` permission and no network code.** This is the project's main promise. Do not add analytics, crash reporting, remote rules or any library that pulls in networking.
- **Do not read text.** The service compares view ids. The only exception is `allowTabs`, which reads the bottom tab bar's labels. Do not widen this.
- **Bump `version` in `rules.json`** whenever you change the default rules. Without it, installed phones never get the change.
- **Update the Rule status table in `README.md`** when you check or fix a rule.
- Keep the code style: small files, comments only where the reason is not obvious, no new dependencies without a reason in the PR.

## Commands

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

JDK 17 or newer. Lint must pass with no errors; CI runs the same command.

## Fixing or adding a rule

Follow `.claude/skills/fix-rule/SKILL.md`. Agents without skill support can read that file directly; it is plain Markdown.

## Things that look like bugs but are not

- After reinstalling Feedcut, a running Facebook sends no events until it is closed once.
- On Xiaomi/HyperOS, Feedcut stops after a few minutes unless its battery setting is *No restrictions*. Check this before debugging the code: `adb shell cat /sys/fs/cgroup/apps/uid_<uid>/pid_<pid>/cgroup.events` shows `frozen 1` when the system froze it.
- Facebook has no view ids at all. That is why it uses `allowTabs` instead of `hints`.
