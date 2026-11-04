---
name: fix-rule
description: Fix a Kapu rule that stopped working after an app update, or add a rule for a new app or screen. Use when Kapu no longer leaves Reels, Shorts, TikTok or a Facebook tab, when it closes a screen it should not, or when someone wants to block a new app's screen. Needs a phone or emulator over adb.
---

# Fix or add a Kapu rule

Work through the steps in order. Most "broken rule" reports are a device problem (step 1), not a rule problem.

## 1. Rule out the device

```bash
adb shell settings get secure enabled_accessibility_services   # must contain io.github.yxonyx.kapu/io.github.yxonyx.kapu.GateService
adb shell pidof io.github.yxonyx.kapu   # empty: Kapu is not running
```

- On Xiaomi/HyperOS check that the system has not frozen Kapu. Get the uid from `adb shell dumpsys package io.github.yxonyx.kapu | grep userId=`, then `adb shell cat /sys/fs/cgroup/apps/uid_<uid>/pid_<pid>/cgroup.events`. `frozen 1` means the battery setting is wrong, not the rule. Kapu needs *No restrictions* and *Autostart on*.
- Close the target app once (`adb shell am force-stop <package>`). A copy that was running when Kapu was installed sends no events.

If any of these was the cause, stop here and tell the user.

## 2. Capture the screen

Open the screen that should be blocked, then:

```bash
adb shell uiautomator dump /sdcard/screen.xml && adb pull /sdcard/screen.xml /tmp/blocked.xml
grep -o 'resource-id="[^"]*"' /tmp/blocked.xml | sort -u
```

If the dump fails with "could not get idle state", pause the video and try again. Capture a screen that must stay usable too (the Instagram feed, a normal YouTube video) into `/tmp/allowed.xml`.

The dumps contain names and message text. Never commit them, paste them into an issue or PR, or quote their text. Only resource ids may leave the machine.

## 3. Pick the marker

For `hints` (apps with view ids: Instagram, YouTube, TikTok):

- Choose an id that is on the blocked screen and not on the allowed one: `comm -23 <(grep -o 'resource-id="[^"]*"' /tmp/blocked.xml | sort -u) <(grep -o 'resource-id="[^"]*"' /tmp/allowed.xml | sort -u)`.
- Prefer the container of the whole screen (`clips_viewer`, `reel_watch_player`) over a button inside it.
- Write the part after `:id/`. Kapu first looks up `<package>:id/<hint>` exactly, which is fast, then falls back to a case-insensitive substring match on visible views. A short, generic hint such as `video` matches far too much. Do not use one.
- Only visible views count. An id that exists but is hidden does not trigger.

For `allowTabs` (apps without view ids, today only Facebook):

- Find the bottom tab bar: nodes with `class="android.app.ActionBar$Tab"`. Their `content-desc` looks like `Marketplace, tab 4 of 6`. Kapu uses the part before the first comma.
- If the user's language names the tab differently, add that label to `allowTabs`. Do not replace the existing one.

To add a new app, get its package name with `adb shell dumpsys window | grep mCurrentFocus` and add an entry with `"enabled": false`. New apps are opt-in.

## 4. Edit and bump

- Change `app/src/main/assets/rules.json`.
- Increase its top-level `version` by one. Without this, installed phones keep the old rule.
- If you changed logic in `Rules.kt`, add a test in `RulesTest.kt`.

## 5. Verify on the device

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop <package>
```

Open the blocked screen. Kapu must leave it within about a second. The debug build keeps a counter you can read:

```bash
adb shell run-as io.github.yxonyx.kapu cat shared_prefs/kapu_count.xml
```

The number goes up by one for each screen Kapu closed. Then open the allowed screen and check that Kapu leaves it alone for at least ten seconds.

## 6. Report

- Update the *Rule status* row in `README.md`: today's date, the phone, the Android version, the app version (`adb shell dumpsys package <package> | grep versionName`).
- In the PR, write the app version, the phone, and the old and new marker. No screenshots of private content.
- If you could not verify on a device, say so in the PR. Do not mark the rule as checked.
