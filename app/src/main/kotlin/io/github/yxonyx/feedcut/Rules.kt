package io.github.yxonyx.feedcut

/**
 * Decisions without the view tree. The service passes only visible view ids,
 * never text.
 */
data class SeenNode(
    val viewId: String?,
    val visible: Boolean,
)

enum class GateAction {
    BACK,
    HOME,
}

data class AppRule(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val action: String,
    val hints: List<String>,
    val allowTabs: List<String> = emptyList(),
    /** What the rule removes, in a few words for the main screen ("Reels", "Only Marketplace"). */
    val note: String = "",
    /** Minutes a day the screen may stay open before it is cut. 0: cut it at once. */
    val dailyMinutes: Int = 0,
)

/** A tab from the bottom bar: only the first part of its label ("Marketplace") and whether it is selected. */
data class SeenTab(
    val label: String,
    val selected: Boolean,
)

/** version numbers the default rules. It tells Feedcut to merge after an app update. */
data class RuleSet(
    val apps: List<AppRule>,
    val version: Int = 0,
)

/**
 * Feedcut never acts on these, whatever the rules file says: itself, Settings, the system UI
 * and the package installer. The user must always be able to switch Feedcut off or uninstall it.
 */
val PROTECTED_PACKAGES = setOf(
    BuildConfig.APPLICATION_ID,
    "com.android.settings",
    "com.android.systemui",
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
)

fun activeHints(rule: AppRule): List<String> =
    if (!rule.enabled) emptyList() else rule.hints.map { it.trim() }.filter { it.isNotEmpty() }

/**
 * If Feedcut had to leave the same app moments ago, Back was not enough
 * (for example the user tapped straight back in), so go Home.
 */
fun escalate(action: GateAction, sameAppJustNow: Boolean): GateAction =
    if (sameAppJustNow) GateAction.HOME else action

fun actionOf(rule: AppRule): GateAction =
    if (rule.action == "home") GateAction.HOME else GateAction.BACK

fun idMatches(hints: List<String>, viewId: String?): Boolean =
    viewId != null && hints.any { hint -> hint != "*" && viewId.contains(hint, ignoreCase = true) }

/**
 * Full view ids for the fast lookup. The target app resolves them in its own
 * process in one call, instead of one call per view.
 */
fun exactIds(packageName: String, hints: List<String>): List<String> =
    hints.filter { it != "*" }.map { if (':' in it) it else "$packageName:id/$it" }

fun chooseAction(
    rule: AppRule,
    windowJustChanged: Boolean,
    nodes: List<SeenNode>,
): GateAction? {
    val hints = activeHints(rule)
    if (hints.isEmpty()) return null
    if (hints.any { it == "*" }) {
        return if (windowJustChanged) actionOf(rule) else null
    }
    val hit = nodes.any { node -> node.visible && idMatches(hints, node.viewId) }
    return if (hit) actionOf(rule) else null
}

fun limitMs(rule: AppRule): Long = rule.dailyMinutes * MS_PER_MINUTE

/** True on the step that starts the last minute, so the warning shows once a day. */
fun limitWarns(limit: Long, usedBefore: Long, usedNow: Long): Boolean =
    limit - usedBefore > MS_PER_MINUTE && limit - usedNow <= MS_PER_MINUTE

fun tabLabel(contentDescription: CharSequence?): String =
    contentDescription?.toString()?.substringBefore(',')?.trim().orEmpty()

/**
 * Which tab to tap. A tab bar is recognised by containing an allowed tab.
 * Without one (menu, group, ad) the screen is left alone.
 */
fun tabRedirect(allow: List<String>, tabs: List<SeenTab>): Int? {
    if (allow.isEmpty()) return null
    val allowed = tabs.indexOfFirst { tab -> allow.any { it.equals(tab.label, ignoreCase = true) } }
    if (allowed < 0) return null
    val selected = tabs.firstOrNull { it.selected } ?: return null
    if (allow.any { it.equals(selected.label, ignoreCase = true) }) return null
    return allowed
}

/**
 * On update, merges the new default rules into the user's rules.
 * base is the default state the user's file started from. Where the user changed
 * a field, the user's value stays; elsewhere the new default wins.
 * Without a base (old installs) the switch and the action stay and the hints update.
 * enabled and dailyMinutes always stay the user's.
 */
fun mergeRules(base: RuleSet?, user: RuleSet, bundled: RuleSet): RuleSet {
    val userByPkg = user.apps.associateBy { it.packageName }
    val baseByPkg = base?.apps?.associateBy { it.packageName }.orEmpty()
    val merged = ArrayList<AppRule>()
    for (fresh in bundled.apps) {
        val mine = userByPkg[fresh.packageName]
        val old = baseByPkg[fresh.packageName]
        if (mine == null) {
            // It was in base, so the user deleted it. Do not bring it back.
            if (old == null) merged.add(fresh)
            continue
        }
        if (base != null && old == null) {
            // The user wrote this rule before it became a default. It stays theirs.
            merged.add(mine)
            continue
        }
        fun <T> pick(field: (AppRule) -> T, keepWithoutBase: Boolean): T = when {
            old == null -> if (keepWithoutBase) field(mine) else field(fresh)
            field(mine) != field(old) -> field(mine)
            else -> field(fresh)
        }
        merged.add(
            AppRule(
                packageName = fresh.packageName,
                label = pick(AppRule::label, keepWithoutBase = false),
                enabled = mine.enabled,
                action = pick(AppRule::action, keepWithoutBase = true),
                hints = pick(AppRule::hints, keepWithoutBase = false),
                allowTabs = pick(AppRule::allowTabs, keepWithoutBase = false),
                note = pick(AppRule::note, keepWithoutBase = false),
                dailyMinutes = mine.dailyMinutes,
            )
        )
    }
    val bundledPkgs = bundled.apps.map { it.packageName }.toSet()
    merged.addAll(user.apps.filter { it.packageName !in bundledPkgs })
    return RuleSet(merged, bundled.version)
}

enum class RuleHealth {
    NOT_INSTALLED,
    NEVER_BLOCKED,
    OK,
    APP_UPDATED,
}

/**
 * Offline hint that a rule may be broken. If the app updated since the last block
 * and nothing was blocked since, the marker may no longer exist.
 */
fun ruleHealth(installedVersion: Long?, lastBlockAt: Long?, versionAtBlock: Long?): RuleHealth = when {
    installedVersion == null -> RuleHealth.NOT_INSTALLED
    lastBlockAt == null -> RuleHealth.NEVER_BLOCKED
    versionAtBlock != null && versionAtBlock != installedVersion -> RuleHealth.APP_UPDATED
    else -> RuleHealth.OK
}

enum class GateState {
    OFF,
    STUCK,
    ON,
}

/** Switched on in Settings is not enough: Android has to be running the service too. */
fun gateState(enabled: Boolean, running: Boolean): GateState = when {
    !enabled -> GateState.OFF
    !running -> GateState.STUCK
    else -> GateState.ON
}

/**
 * Minutes a closed screen is assumed to save. Feedcut cannot know how long someone would
 * have scrolled, so every figure built on this is shown as an estimate with this number.
 */
const val MINUTES_PER_CUT = 1

const val MS_PER_MINUTE = 60_000L
const val MAX_DAILY_MINUTES = 24 * 60

fun minutesKept(cuts: Int): Int = cuts * MINUTES_PER_CUT
