# Contributing

Thanks for helping. Feedcut is maintained by one person in their free time, so small, focused changes get merged fastest.

## What is welcome

- **Rule fixes and new rules.** These are the most useful contributions. Follow [the fix-rule guide](.claude/skills/fix-rule/SKILL.md), bump `version` in `rules.json` and update the *Rule status* table in the README.
- **Reports from other phones.** Open an issue with the phone, the Android version and what worked.
- **Translations.** Add `app/src/main/res/values-<lang>/strings.xml`.
- **Bug fixes** with a test where the logic allows one.

## Open an issue first for

- New features, new settings, UI changes.
- Anything that adds a dependency.

## Will not be merged

- Network access of any kind: analytics, crash reporting, remote rules, update checks. Feedcut has no `INTERNET` permission, and that stays.
- Reading screen text beyond the tab bar labels that `allowTabs` already uses.
- Making Feedcut hard to switch off. It is a habit aid, not parental control.

## Pull requests

- One change per PR.
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` must pass. CI runs the same.
- Say how you tested it: phone, Android version, app version. If you could not test on a device, say that.
- Never include screen dumps or screenshots with personal content.
- Contributions are accepted under the project's license.

Using a coding agent is fine. Point it at [`AGENTS.md`](AGENTS.md), and check what it wrote before you open the PR.
