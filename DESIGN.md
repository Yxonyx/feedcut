---
name: Feedcut
description: The feed, cut. A trail blaze painted on granite tells you whether the gate holds.
colors:
  granite: "#17191A"
  granite-raised: "#202223"
  limestone: "#EDEBE4"
  limestone-raised: "#F7F6F1"
  blaze-white: "#F4F2EC"
  blaze-red: "#D7261E"
  blaze-red-deep: "#B81F18"
  blaze-red-night: "#CF231B"
  trail-yellow: "#F2B84B"
  ochre: "#7A4E00"
  lichen: "#55574F"
  lichen-night: "#A3A59F"
  hairline: "#CFCCC2"
  hairline-night: "#303332"
  moss: "#4B6B45"
  moss-night: "#5E7F57"
typography:
  display:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "112sp"
    fontWeight: 800
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "24sp"
    fontWeight: 700
    letterSpacing: "0.08em"
  title-count:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "22sp"
    fontWeight: 700
  title-status:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "21sp"
    fontWeight: 700
  label-button:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "16sp"
    fontWeight: 700
    letterSpacing: "0.04em"
  label-section:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "15sp"
    fontWeight: 700
    letterSpacing: "0.1em"
  label-data:
    fontFamily: "Barlow Condensed, sans-serif-condensed"
    fontSize: "15sp"
    fontWeight: 600
  body:
    fontFamily: "sans-serif, Roboto"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.03125em"
  body-small:
    fontFamily: "sans-serif, Roboto"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.0179em"
rounded:
  waymark: "2dp"
  blaze: "3dp"
spacing:
  hair: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  step: "14dp"
  lg: "16dp"
  gutter: "20dp"
  xl: "24dp"
  section: "28dp"
  section-wide: "36dp"
components:
  button-primary:
    backgroundColor: "{colors.blaze-red-deep}"
    textColor: "{colors.blaze-white}"
    typography: "{typography.label-button}"
    rounded: "{rounded.blaze}"
    height: "40dp"
    padding: "0 24dp"
  button-primary-night:
    backgroundColor: "{colors.blaze-red-night}"
    textColor: "{colors.blaze-white}"
    typography: "{typography.label-button}"
    rounded: "{rounded.blaze}"
    height: "40dp"
    padding: "0 24dp"
  button-outlined:
    textColor: "{colors.granite}"
    typography: "{typography.label-button}"
    rounded: "{rounded.blaze}"
    height: "40dp"
    padding: "0 24dp"
  button-outlined-night:
    textColor: "{colors.blaze-white}"
    typography: "{typography.label-button}"
    rounded: "{rounded.blaze}"
    height: "40dp"
    padding: "0 24dp"
  status-blaze:
    backgroundColor: "{colors.blaze-white}"
    rounded: "{rounded.blaze}"
    height: "40dp"
    width: "100%"
  status-line:
    textColor: "{colors.granite}"
    typography: "{typography.title-status}"
  app-row:
    textColor: "{colors.granite}"
    typography: "{typography.body}"
    height: "68dp"
    padding: "10dp 0"
  app-row-detail:
    textColor: "{colors.lichen}"
    typography: "{typography.body-small}"
  app-row-detail-warn:
    textColor: "{colors.ochre}"
    typography: "{typography.body-small}"
  section-heading:
    textColor: "{colors.lichen}"
    typography: "{typography.label-section}"
  waymark:
    rounded: "{rounded.waymark}"
    width: "14dp"
    height: "20dp"
  week-row:
    textColor: "{colors.lichen}"
    typography: "{typography.label-data}"
    height: "26dp"
  week-bar:
    backgroundColor: "{colors.moss}"
    height: "8dp"
  week-bar-today:
    backgroundColor: "{colors.blaze-red-deep}"
    height: "8dp"
  advanced-row:
    textColor: "{colors.granite}"
    typography: "{typography.body}"
    height: "56dp"
---

# Design System: Feedcut

## Overview

**Creative North Star: "The Trail Blaze on Granite"**

Feedcut is a trail sign, not a dashboard. Its one job on screen is to say whether the gate holds, and it says it the way a mountain trail does: a white-red-white blaze painted on stone. Whole paint means Feedcut is cutting the feeds; the same three bands drawn but unpainted mean it is off; a blaze snapped in two and knocked out of line, painted trail yellow, means Android stopped it. A sentence under the blaze always says the same thing in words.

The ground is stone: granite (#17191A) in dark mode, a pale limestone in light mode, both first-class. Signage is set in Barlow Condensed, the narrow bold face of painted trail boards, and everything people read as sentences stays in the system sans. The screen is a single ruled column: hairline-separated rows, no cards, no elevation, generous gaps between sections. The day's count is the one oversized element, cut in 112sp condensed numerals, and the last seven days sit on a fixed scale so a bar's length is its minutes. The world refuses the category default of a purple Material card with a big number.

Density is low and the tone is strict. The screen is opened rarely and glanced at, so status, count and history are visible without scrolling once setup is done; setup itself appears as a short trail of waymarks only until it is finished.

**Key Characteristics:**
- One painted blaze carries the status: ON (whole), OFF (unpainted outline), STUCK (snapped, trail yellow).
- Paint is scarce: blaze red and white appear where Feedcut works or where the next step is, nowhere else.
- Barlow Condensed for signage, system sans for reading.
- Flat granite or limestone ground, 1dp hairlines instead of cards, no shadows.
- Cut-board corners (3dp, 2dp at waymark size); no pills on owned components.
- History on a fixed 120-minute scale, past days in moss, today in red.

## Colors

Stone grounds, two blaze paints, one warning yellow and one quiet moss; light and dark are separate schemes built from the same materials.

### Primary
- **Blaze Red** (`blaze-red`): the red band of the blaze, the done waymark's band, and the primary color in dark mode (filled button, switch track, today's week bar). It is a fixed world color; the painted blaze and waymark use it in both modes.
- **Deep Blaze Red** (`blaze-red-deep`): the light-mode primary. Filled button, switch track and today's week bar on limestone, darkened from Blaze Red so white labels on it hold contrast.
- **Night Blaze Red** (`blaze-red-night`): the dark-mode primary for the same jobs. A shade under Blaze Red so blaze-white labels reach 4.8:1; the status blaze itself keeps Blaze Red.
- **Blaze White** (`blaze-white`): the white bands of the blaze, the label on every filled button, and ink in dark mode.

### Secondary
- **Moss** (`moss`, light) and **Night Moss** (`moss-night`, dark): past days' bars in the week view. History only. The direction named a darker moss (#3E5B3A); the build ships these lighter values, and the build is the record.

### Tertiary
- **Trail Yellow** (`trail-yellow`): the snapped STUCK blaze, and the warning text color in dark mode.
- **Ochre** (`ochre`): the light-mode warning text, used for an app row whose rule may have broken after an app update.

### Neutral
- **Granite** (`granite`): dark-mode ground and window, light-mode ink, launcher icon background.
- **Raised Granite** (`granite-raised`): dark-mode container surface (Material `surfaceContainerHigh`: dialogs and menus).
- **Limestone** (`limestone`): light-mode ground and window.
- **Raised Limestone** (`limestone-raised`): light-mode container surface (dialogs and menus).
- **Lichen** (`lichen`, light) and **Night Lichen** (`lichen-night`, dark): secondary text, section headings, outlined-button stroke, the unpainted OFF blaze and todo waymark, unselected week labels, not-installed app names.
- **Hairline** (`hairline`, light) and **Night Hairline** (`hairline-night`, dark): 1dp row rules, the zero-day stub in the week view, the switch's unchecked track.

### Named Rules
**The Scarce Paint Rule.** Blaze red and blaze white mark only what is working or what to do next: the ON blaze, a done waymark, the one filled button, a checked switch, today's bar. No red headings, red icons or red decoration.

**The Yellow Means Check Rule.** Trail yellow and ochre appear only when something needs checking: the STUCK blaze and a "rule may be broken" detail line. Never as an accent.

**The Moss Is History Rule.** Past days are moss; only today is red. Moss is not used for success, status or buttons.

## Typography

**Display Font:** Barlow Condensed (SemiBold 600, Bold 700, ExtraBold 800, bundled; falls back to sans-serif-condensed)
**Body Font:** the system sans-serif through the Material 3 type scale (Roboto on stock Android)

**Character:** Barlow Condensed is trail-board lettering: narrow, bold, legible at a glance. The system sans keeps instructions and details quiet and familiar, and follows the device's font.

### Hierarchy
- **Display** (800, 112sp, -0.02em, font padding off): the day's count only. Negative margins trim Barlow's ascent and descent so the numerals' foot sits on the bottom of the label column beside it.
- **Headline** (700, 24sp, 0.08em): the FEEDCUT wordmark (written uppercase in the string) and screen titles such as Advanced.
- **Title, count** (700, 22sp): "feeds cut today" and "≈ N min kept" beside the count.
- **Title, status** (700, 21sp): the status sentence under the blaze. A polite live region.
- **Body** (400, 16sp/24sp): app names, setup step titles, the Advanced row, empty states.
- **Body small** (400, 14sp/20sp, lichen): detail lines, the estimate note, setup state, the privacy line.
- **Label, button** (700, 16sp, 0.04em, uppercase): every Feedcut button.
- **Label, section** (700, 15sp, 0.1em, uppercase, lichen): section headings (Setup, Last 7 days, Apps), marked as accessibility headings.
- **Label, data** (600, 15sp): day names and minute values in the week view; lichen for past days, ink for today.

### Named Rules
**The Signage and Reading Rule.** Barlow Condensed is for signs: the wordmark, status, count, section headings, buttons and week figures. Anything read as a sentence (details, instructions, notes) is in the system sans. Uppercase is used only for the wordmark, section headings and button labels.

**The Honest Number Rule.** Estimates are marked in the type itself: "≈" leads the minutes, and the estimate note sits directly under it in body small.

## Layout

One scrolling column with a 20dp side gutter (`gutter`), 12dp top and 24dp bottom padding, drawn edge to edge behind transparent system bars with insets applied as padding. No grid, no breakpoints: the column fills the phone width.

Top to bottom: wordmark left and setup state right on one line; the blaze, full width and 40dp tall, 20dp below; the status sentence 12dp under it; the setup trail (only while setup is unfinished); the count block; the week; the app rows; the Advanced row; the privacy line. Section breaks are space, not boxes: 28dp before the setup and count blocks (`section`), 20dp before the week heading, 36dp before the apps heading (`section-wide`), 10dp from week heading to bars. The count's label column sits 16dp right of the numerals.

Rows are ruled by 1dp hairlines. App rows are at least 68dp tall with 10dp vertical padding, name over detail with 2dp between, switch at the end with 12dp lead. The Advanced row is 56dp, ruled above and below. Setup steps are 14dp apart, a 14x20dp waymark with 14dp gap before the text; action buttons stack full width 8dp below the detail, 4dp apart.

### Named Rules
**The Fixed Scale Rule.** In the week view, 120 minutes is the full bar width on every day, in every week. Longer days are drawn full and labelled "N min+". Never scale bars to the week's maximum.

**The Hairline Not Card Rule.** Group with 1dp hairlines and space. Content sits directly on the ground.

## Elevation & Depth

Flat. No shadows, no elevated cards, no tonal surface stacks on the main screens. Depth is conveyed only by 1dp hairlines between rows and by the 1.5dp lichen edge around the blaze and waymarks. Material dialogs keep their own container surface (raised granite or raised limestone).

### Named Rules
**The Painted Not Lifted Rule.** Things are painted on the stone, never floated above it. State changes change paint, never elevation.

## Shapes

Cut boards. Owned shapes are rectangles with small, even corners: 3dp (`blaze`) on the blaze and on every Feedcut button, 2dp (`waymark`) on setup waymarks, 1.5 units on the launcher icon's boards at the 108-unit grid. Bands inside the blaze are square-cut; the OFF blaze draws its two band lines at 30% and 70% of its height; the STUCK blaze splits with a gap of 6% of its width and drops the right half by 14% of its height. The one round shape is the standard Material switch, kept as the platform control.

### Named Rules
**The Cut Board Rule.** Feedcut's own shapes (buttons, blaze, waymarks, icon boards) have 2 to 3dp corners. No pills, no circles, no large radii on anything the app draws; platform switches are the exception.

## Components

### Buttons
Signboard buttons: bold condensed uppercase labels on cut boards.
- **Shape:** cut-board corners (3dp), Material 3 height (40dp visual, 48dp touch).
- **Primary (filled):** deep blaze red in light mode, blaze red in dark mode, blaze-white label. Used only for the next step to take; the screen code allows one per screen and makes every other action outlined.
- **Outlined:** transparent fill, ink label, 1dp lichen stroke. Secondary setup actions and Advanced actions (Rules file, Restore default rules).
- **Hover / Focus:** Material ripple and state layers; no custom transitions.
- **Layout:** in setup and Advanced, buttons span the column width and stack.

### Switches
- **Style:** standard Material 3 switch. Checked track takes the primary red; unchecked track is hairline with a lichen outline.
- **Use:** one per installed app row, labelled with the app name for accessibility. Hidden for apps that are not installed.

### Status Blaze (signature)
A full-width, 40dp custom view drawn in code, always paired with the status sentence below it (the sentence carries the meaning; the view is hidden from accessibility).
- **ON:** white band, red band, white band (30/40/30 split), 3dp corners, 1.5dp lichen edge.
- **OFF:** same outline and two band lines in lichen, no paint.
- **STUCK:** two halves with a gap, right half dropped; white-yellow-white bands, each half edged in lichen.

### Today Count
The display numeral beside a stacked label column: "feeds cut today", "≈ N min kept" (title, count), then "Estimate: 1 min per cut." (body small). Numerals use the device's number format.

### Week View
Seven 26dp rows: short day name (44dp column), an 8dp bar, the minutes right-aligned (64dp column). Past bars moss, today's bar primary red, today's text in ink. A zero day draws a 3x2dp hairline stub. Hidden behind "No cuts this week." when every day is zero.

### App Rows
Hairline-ruled list. App name in body, detail in body small built as fixed furniture: what the rule removes, a middle dot, then its state ("last cut 4 min ago", "Nothing cut yet", "Off", "Not installed"). A possibly broken rule turns the detail ochre (trail yellow at night) and says so in words. Not-installed apps grey their name to lichen and lose the switch, sorted last.

### Setup Waymarks
Each step is a 14x20dp waymark beside a title, an optional detail and its actions. A done step's waymark is a painted blaze (blaze white with a blaze red band, lichen edge, 2dp corners); a todo step's is the outline with two band lines, unpainted. The block disappears once the checkable steps are done; the header then reads "Setup done".

### Navigation
No app bar, tabs or nav drawer. Secondary screens are reached from one full-width, 56dp ruled text row ("Advanced: screen markers and rules file") at the end of the main column, with the standard ripple. The Advanced screen reuses the column, gutter and headline style; system Back returns.

### Advanced Screen
The same column, gutter and headline as the main screen, in three sections set apart by space and section labels: Screen markers, Rules file, Your phone. It ends with a body-small version line ("Feedcut 1.1.0 · rules version 4").
- **Rule rows:** hairline-ruled like app rows. App name in body over the package in body small (one line, middle-ellipsized), the Back/Home toggle at the end, the markers field below.
- **Back/Home toggle:** two outlined segments with cut-board corners and condensed uppercase labels. The chosen segment is painted ink with a ground-coloured label; the other stays bare. Never red: this is a setting, not the next step.
- **Markers field:** Material outlined text field with 3dp corners, lichen stroke turning ink on focus. The ids are monospace; the floating label stays in the system sans.
- **Tab rules:** a rule with `allowTabs` (Facebook) shows no toggle and no field, only a body-small line naming the cut tabs and the tab they open, because Back, Home and markers do not apply to it.

### Rules File Editor
Headline, one body-small line saying that a file that cannot be read is not saved, then the raw JSON on a cut board (surface paint, 1dp hairline edge, 3dp corners, 12dp padding, 13sp monospace). The filled Save button sits below it; it is the screen's one next step. Back with unsaved edits asks first.

### Dialogs
Material alert dialogs with cut-board corners (3dp). Both buttons use the condensed uppercase label face in ink. Red text is not used here, since red on the dialog surface falls under 4.5:1 in the dark scheme.

### Home Screen Widget
The main screen's Today Count at home screen size, on the ground colour of the current scheme with a 1dp hairline edge. A display numeral beside a label column leads: the limit that runs out first, as minutes left, the app below in body small, a 14x20dp waymark at the top end and a 6dp bar of what is left in the primary red. Other limits follow as ruled rows, app name in the system sans, time in the condensed bold. Without a limit, the numeral is the count of today's cuts with "≈ N min kept" below it. A spent limit says "Used up" and empties the bar; it is not a warning, so it stays out of trail yellow.
- **Signage is painted:** launchers cannot load Barlow Condensed, so the numeral, the unit and the row times are drawn into white bitmaps and tinted from the layout. The tint keeps light and dark following the launcher, and each image carries its text as a content description.
- **Corners:** the system's widget radius from Android 12, because the board sits among the launcher's own widgets and not all launchers clip it; the cut-board 3dp below that.
- **Below 140dp high:** only the numeral, its labels and the bar.

### Launcher Mark
An adaptive icon on granite: three blaze-white boards (a feed of posts) sliced on a 28 degree cut, the halves slid apart along it, with a blaze red band lying in the cut. The monochrome layer leaves the cut empty. Icon layers and `docs/logo.svg` are generated from one geometry by `tools/make_icon.py`; change the script, not its output.

## Do's and Don'ts

### Do:
- **Do** pair every colored state with words: the status sentence under the blaze, the detail line on an app row.
- **Do** keep exactly one filled button on a screen, for the next step; make the rest outlined.
- **Do** keep the week view on its fixed 120-minute scale and mark overflow with "min+".
- **Do** set signage in Barlow Condensed and sentences in the system sans.
- **Do** separate rows with 1dp hairlines and sections with space (20, 28 or 36dp).
- **Do** use 3dp corners on buttons and blaze shapes, 2dp on waymark-sized marks.
- **Do** design light and dark together from the same materials: limestone and granite grounds, deep blaze red and blaze red primaries.
- **Do** lead any estimated number with "≈" and keep its assumption visible next to it.

### Don't:
- **Don't** put content in cards, add shadows or raise elevation on the main screens.
- **Don't** use blaze red for decoration, headings or icons.
- **Don't** use trail yellow or ochre for anything that is not a "check this" state.
- **Don't** use pill or circular shapes on Feedcut's own buttons, marks or icon; platform switches are the only round control.
- **Don't** auto-scale the week bars to the largest day.
- **Don't** use purple or default Material tonal palettes; colors come from the stone, the blaze paints, moss and trail yellow.
- **Don't** show the setup trail once setup is done.
