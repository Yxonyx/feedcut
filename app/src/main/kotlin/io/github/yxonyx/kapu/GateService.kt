package io.github.yxonyx.kapu

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Csak a szabályfájlban engedélyezett csomagokat figyeli.
 * Szöveget és képet nem olvas, és nem küld sehova. Leírást csak az allowTabs
 * szabálynál olvas: az alsó fülsor feliratát, más nézetét nem.
 */
class GateService : AccessibilityService() {

    private var rulesAt = Long.MIN_VALUE
    private var rules: List<AppRule> = emptyList()
    private var lastActionAt = 0L

    // Egy videó másodpercenként sok contentChanged eseményt küld. Ezeket egy
    // ellenőrzéssé vonjuk össze, hogy ne álljanak sorba a fa-bejárások.
    private val handler = Handler(Looper.getMainLooper())
    private var pendingPackage: String? = null
    private var checkPending = false
    private val check: Runnable = Runnable {
        checkPending = false
        val target = pendingPackage ?: return@Runnable
        // Közvetlenül egy lépés után nem dönt, hanem a védőidő végén újranéz.
        // Különben egy gyors visszalépés elveszne, ha az app nem küld újabb eseményt.
        val wait = DEBOUNCE_MS - (SystemClock.elapsedRealtime() - lastActionAt)
        if (wait > 0) {
            checkPending = true
            handler.postDelayed(check, wait)
            return@Runnable
        }
        pendingPackage = null
        val rule = currentRules().firstOrNull { it.packageName == target } ?: return@Runnable
        if (rule.allowTabs.isNotEmpty()) {
            redirectTabs(rule)
        } else if (seesHint(rule)) {
            act(actionOf(rule))
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        applyPackages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applyPackages()
        return START_NOT_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val rule = currentRules().firstOrNull { it.packageName == packageName } ?: return
        val hints = activeHints(rule)
        val tabsOnly = rule.enabled && rule.allowTabs.isNotEmpty()
        if (hints.isEmpty() && !tabsOnly) return
        val windowJustChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (hints.any { it == "*" }) {
            chooseAction(rule, windowJustChanged, emptyList())?.let(::act)
            return
        }
        pendingPackage = packageName
        if (windowJustChanged || !checkPending) {
            handler.removeCallbacks(check)
            checkPending = true
            handler.postDelayed(check, if (windowJustChanged) 0L else SETTLE_MS)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(check)
        super.onDestroy()
    }

    private fun act(action: GateAction, tab: AccessibilityNodeInfo? = null) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionAt < DEBOUNCE_MS) return
        val performed = tab?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true ||
            when (action) {
                GateAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                GateAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            }
        if (performed) {
            lastActionAt = now
            DayCount.bump(this)
        }
    }

    private fun currentRules(): List<AppRule> {
        val target = RuleStore.file(this)
        val modified = if (target.exists()) target.lastModified() else 0L
        if (modified != rulesAt) {
            rules = RuleStore.readQuiet(this).apps
            rulesAt = modified
            applyPackages()
        }
        return rules
    }

    private fun applyPackages() {
        val info = serviceInfo ?: return
        val watched = currentRules()
            .filter { it.enabled }
            .map { it.packageName }
            .toMutableSet()
        watched.add(packageName)
        info.packageNames = watched.toTypedArray()
        serviceInfo = info
    }

    private fun seesHint(rule: AppRule): Boolean {
        val hints = activeHints(rule)
        if (hints.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        try {
            if (root.packageName?.toString() != rule.packageName) return false
            for (id in exactIds(rule.packageName, hints)) {
                val found = root.findAccessibilityNodeInfosByViewId(id)
                val hit = found.any { it.isVisibleToUser }
                found.forEach { it.recycle() }
                if (hit) return true
            }
            // Tartalék a részleges jelekhez: csak a látható ágakat járja be,
            // és az első találatnál megáll.
            return walk(root, hints, intArrayOf(MAX_NODES), recycleSelf = false)
        } finally {
            root.recycle()
        }
    }

    /** Ha a fülsoron tiltott fül van kijelölve, átkattint az engedélyezettre. */
    private fun redirectTabs(rule: AppRule) {
        val root = rootInActiveWindow ?: return
        val tabs = ArrayList<AccessibilityNodeInfo>()
        try {
            if (root.packageName?.toString() != rule.packageName) return
            collectTabs(root, tabs, intArrayOf(MAX_NODES), recycleSelf = false)
            val seen = tabs.map { SeenTab(tabLabel(it.contentDescription), it.isSelected) }
            val target = tabRedirect(rule.allowTabs, seen) ?: return
            act(actionOf(rule), tabs[target])
        } finally {
            tabs.forEach { it.recycle() }
            root.recycle()
        }
    }

    private fun collectTabs(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>,
        budget: IntArray,
        recycleSelf: Boolean,
    ) {
        var keep = false
        try {
            // A Facebook görgetéskor elrejti az alsó sávot, ezért itt a rejtett ágat is nézzük.
            if (budget[0] <= 0) return
            budget[0]--
            if (node.className?.toString() == TAB_CLASS) {
                out.add(node)
                keep = true
                return
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectTabs(child, out, budget, recycleSelf = true)
            }
        } finally {
            if (recycleSelf && !keep) node.recycle()
        }
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        hints: List<String>,
        budget: IntArray,
        recycleSelf: Boolean,
    ): Boolean {
        try {
            if (budget[0] <= 0 || !node.isVisibleToUser) return false
            budget[0]--
            if (idMatches(hints, node.viewIdResourceName)) return true
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (walk(child, hints, budget, recycleSelf = true)) return true
            }
            return false
        } finally {
            if (recycleSelf) node.recycle()
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 800L
        const val SETTLE_MS = 120L
        const val MAX_NODES = 800
        const val TAB_CLASS = "android.app.ActionBar\$Tab"
    }
}
