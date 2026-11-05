package io.github.yxonyx.feedcut

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches only the packages enabled in the rules file.
 * Reads no text or images and sends nothing anywhere. The only labels it reads are
 * the bottom tab bar's, for allowTabs rules.
 */
class GateService : AccessibilityService() {

    private var rulesAt = Long.MIN_VALUE
    private var rulesCheckedAt = -RULES_CHECK_MS
    private var rules: List<AppRule> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val timer = CheckTimer(SETTLE_MS, DEBOUNCE_MS)
    private var pendingPackage: String? = null
    private var lastActPackage: String? = null
    private var lastActAt = 0L
    private val check: Runnable = Runnable {
        timer.onFire(SystemClock.elapsedRealtime())?.let { wait ->
            handler.postDelayed(check, wait)
            return@Runnable
        }
        val target = pendingPackage ?: return@Runnable
        pendingPackage = null
        // A stale node can throw. That must not stop the service.
        try {
            val rule = currentRules().firstOrNull { it.packageName == target } ?: return@Runnable
            if (rule.allowTabs.isNotEmpty()) {
                redirectTabs(rule)
            } else if (blocksWholeApp(rule) || seesHint(rule)) {
                act(rule)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "check failed", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        applyPackages()
    }

    /** The app calls this after saving rules, so the file is read again right away. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        rulesCheckedAt = -RULES_CHECK_MS
        applyPackages()
        // Only the system's binding should keep the service alive, not this ping.
        stopSelf(startId)
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
        // A whole-app rule only cares about the app opening, not about what it draws.
        if (hints.any { it == "*" } && chooseAction(rule, windowJustChanged, emptyList()) == null) return
        pendingPackage = packageName
        timer.onEvent(windowJustChanged)?.let { delay ->
            handler.removeCallbacks(check)
            handler.postDelayed(check, delay)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(check)
        timer.cancel()
        super.onDestroy()
    }

    private fun act(rule: AppRule, tab: AccessibilityNodeInfo? = null) {
        val now = SystemClock.elapsedRealtime()
        if (!timer.canAct(now)) return
        val sameAppJustNow = rule.packageName == lastActPackage && now - lastActAt < ESCALATE_MS
        // A tab rule only ever taps the tab: Back or Home would throw the user out of the app.
        val performed = if (tab != null) {
            tab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            when (escalate(actionOf(rule), sameAppJustNow)) {
                GateAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                GateAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
        if (performed) {
            timer.acted(now)
            lastActPackage = rule.packageName
            lastActAt = now
            DayCount.bump(this)
            BlockLog.record(this, rule.packageName)
            // Look again once the debounce is over: Back does not always leave the screen.
            pendingPackage = rule.packageName
            timer.onEvent(windowChanged = false)?.let { delay ->
                handler.removeCallbacks(check)
                handler.postDelayed(check, delay)
            }
        }
    }

    /** Checks the file every few seconds at most, not on every event. */
    private fun currentRules(): List<AppRule> {
        val now = SystemClock.elapsedRealtime()
        if (now - rulesCheckedAt < RULES_CHECK_MS) return rules
        rulesCheckedAt = now
        val target = RuleStore.file(this)
        val modified = if (target.exists()) target.lastModified() else 0L
        if (modified != rulesAt) {
            // On a failed read keep the last good rules and try again on the next check.
            val read = RuleStore.readQuiet(this) ?: return rules
            rules = read.apps
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

    /** A "*" rule, and the app is still the one on screen once the debounce is over. */
    private fun blocksWholeApp(rule: AppRule): Boolean {
        if (activeHints(rule).none { it == "*" }) return false
        val root = rootInActiveWindow ?: return false
        try {
            return root.packageName?.toString() == rule.packageName
        } finally {
            root.recycle()
        }
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
            // Fallback for partial hints: walks only visible branches
            // and stops at the first match.
            return walk(root, hints, intArrayOf(MAX_NODES), recycleSelf = false)
        } finally {
            root.recycle()
        }
    }

    /** If a forbidden tab is selected in the tab bar, taps the allowed one. */
    private fun redirectTabs(rule: AppRule) {
        val root = rootInActiveWindow ?: return
        val tabs = ArrayList<AccessibilityNodeInfo>()
        try {
            if (root.packageName?.toString() != rule.packageName) return
            collectTabs(root, tabs, intArrayOf(MAX_NODES), recycleSelf = false)
            val seen = tabs.map { SeenTab(tabLabel(it.contentDescription), it.isSelected) }
            val target = tabRedirect(rule.allowTabs, seen) ?: return
            act(rule, tabs[target])
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
            // Facebook hides the bottom bar while scrolling, so hidden branches are searched too.
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
        const val TAG = "Feedcut"
        const val DEBOUNCE_MS = 800L
        const val SETTLE_MS = 120L
        const val RULES_CHECK_MS = 3000L
        const val ESCALATE_MS = 2500L
        const val MAX_NODES = 800
        const val TAB_CLASS = "android.app.ActionBar\$Tab"
    }
}
