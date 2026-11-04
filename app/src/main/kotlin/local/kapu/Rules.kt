package local.kapu

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

data class RuleSet(
    val apps: List<AppRule>,
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
