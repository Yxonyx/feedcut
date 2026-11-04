package io.github.yxonyx.kapu

/**
 * Döntés a képernyőfa nélkül. A szolgáltatás csak látható view-id-ket ad át,
 * szöveget nem.
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
)

/** Egy fül az app alsó sávjából: csak a felirat első tagja ("Marketplace") és a kijelölés. */
data class SeenTab(
    val label: String,
    val selected: Boolean,
)

/** A version a gyári szabályok sorszáma. Ebből tudja a Kapu, hogy frissítés után össze kell fésülni. */
data class RuleSet(
    val apps: List<AppRule>,
    val version: Int = 0,
)

fun activeHints(rule: AppRule): List<String> =
    if (!rule.enabled) emptyList() else rule.hints.map { it.trim() }.filter { it.isNotEmpty() }

fun actionOf(rule: AppRule): GateAction =
    if (rule.action == "home") GateAction.HOME else GateAction.BACK

fun idMatches(hints: List<String>, viewId: String?): Boolean =
    viewId != null && hints.any { hint -> hint != "*" && viewId.contains(hint, ignoreCase = true) }

/**
 * Teljes view-id-k a gyors kereséshez. Ezt a cél app a saját folyamatában
 * keresi ki, egy hívással, nem nézetenként.
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

fun tabLabel(contentDescription: CharSequence?): String =
    contentDescription?.toString()?.substringBefore(',')?.trim().orEmpty()

/**
 * Melyik fülre kell átkattintani. A fülsort az ismeri fel, hogy van benne
 * engedélyezett fül. Ha nincs ilyen (menü, csoport, hirdetés), békén hagyja.
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
 * Frissítéskor a gyári szabályokat összefésüli a felhasználóéval.
 * A base az a gyári állapot, amiből a felhasználó fájlja indult. Ahol a
 * felhasználó eltért tőle, az övé marad, máshol az új gyári érték jön.
 * Ha nincs base (régi telepítés), a kapcsoló és a lépés marad, a jelek frissülnek.
 * A bekapcsolás mindig a felhasználóé.
 */
fun mergeRules(base: RuleSet?, user: RuleSet, bundled: RuleSet): RuleSet {
    val userByPkg = user.apps.associateBy { it.packageName }
    val baseByPkg = base?.apps?.associateBy { it.packageName }.orEmpty()
    val merged = ArrayList<AppRule>()
    for (fresh in bundled.apps) {
        val mine = userByPkg[fresh.packageName]
        val old = baseByPkg[fresh.packageName]
        if (mine == null) {
            // Ha a base-ben megvolt, a felhasználó törölte: nem tesszük vissza.
            if (old == null) merged.add(fresh)
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
 * Offline jelzés arra, hogy egy szabály talán eltört. Ha az app frissült a legutóbbi
 * blokkolás óta, és azóta nem volt blokkolás, a jel lehet, hogy már nem létezik.
 */
fun ruleHealth(installedVersion: Long?, lastBlockAt: Long?, versionAtBlock: Long?): RuleHealth = when {
    installedVersion == null -> RuleHealth.NOT_INSTALLED
    lastBlockAt == null -> RuleHealth.NEVER_BLOCKED
    versionAtBlock != null && versionAtBlock != installedVersion -> RuleHealth.APP_UPDATED
    else -> RuleHealth.OK
}
