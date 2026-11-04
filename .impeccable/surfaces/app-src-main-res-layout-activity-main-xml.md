---
version: 1
slug: "app-src-main-res-layout-activity-main-xml"
primary_target: "app/src/main/res/layout/activity_main.xml"
related_targets: ["app/src/main/res/drawable/ic_launcher.xml"]
---

# Main screen

Scope: Feedcut's main screen (status, today, setup, apps) plus the launcher icon. Mode: Operate.
Audience: adults limiting their own scrolling; opened rarely, glanced at. Job: is it working, how much did it cut today, how much time did that keep. Voice: strict coach.
Constraints: native XML views + Material Components; light and dark first-class; no network; time saved is an estimate and says so.

## Direction contract

THESIS: The feed, cut. Status is a painted trail blaze: whole when Feedcut works, missing when off, broken when Android stopped it. Refuses the category default of a purple Material card with a big number.

OWN-WORLD: Granite ground (#17191A dark, #EDEBE4 light), blaze white #F4F2EC and blaze red #D7261E, trail-yellow #F2B84B only for "check this", moss #3E5B3A for history. Barlow Condensed (signage) for display and labels, Roboto for body. Hairline-ruled rows, no cards, square-ish blaze shapes.

STORY: The user sees at once whether the gate holds, how many feeds were cut today and the estimated minutes kept, the last 7 days at exact scale, then which apps are covered and whether any rule may be broken. Setup appears as waymarks only until done.

FIRST VIEWPORT: Wordmark top left, setup state top right. Full-width blaze (3 bands) under it with one status line. Then the day's number in huge condensed numerals beside "feeds cut today / ≈ N min kept / estimate note". Then 7 day bars, then the app rows with standard switches. Advanced (markers, rules file) behind one row at the end.

FORM: candidate 6 of 7 (trail waymarks), seed: seed:ce451a82183f1cad. Raises: fixed scale and baseline (botanical folio), hairline density (Japanese high-density), fixed per-app furniture (character catalog), bar length equals exact minutes (labanotation).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
