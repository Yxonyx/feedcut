package io.github.yxonyx.feedcut

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView

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
    private val timer = CheckTimer(SETTLE_MS, DEBOUNCE_MS, FOLLOW_UP_MS, FOLLOW_UPS)
    private val clock = ScreenClock(TICK_MS, 2 * TICK_MS)
    private var notice: View? = null
    private val hideNotice: Runnable = Runnable {
        notice?.let { getSystemService(WindowManager::class.java).removeView(it) }
        notice = null
    }
    private var pendingPackage: String? = null
    private var lastActPackage: String? = null
    private var lastActAt = 0L
    private var backsInARow = 0
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
                if (limitIsUsedUp(rule)) act(rule)
            } else {
                clock.stop()
                timer.foundNothing()?.let { delay ->
                    pendingPackage = target
                    handler.postDelayed(check, delay)
                }
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "check failed", e)
        }
    }

    // A paused video sends no events, so a screen with a limit is looked at on a timer too.
    private val tick: Runnable = Runnable {
        val target = clock.watching ?: return@Runnable
        schedule(target)
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
        if (packageName in PROTECTED_PACKAGES) return
        val rule = currentRules().firstOrNull { it.packageName == packageName } ?: return
        val hints = activeHints(rule)
        val tabsOnly = rule.enabled && rule.allowTabs.isNotEmpty()
        if (hints.isEmpty() && !tabsOnly) return
        val windowJustChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        // A whole-app rule only cares about the app opening, not about what it draws.
        if (hints.any { it == "*" } && chooseAction(rule, windowJustChanged, emptyList()) == null) return
        schedule(packageName, windowJustChanged)
    }

    private fun schedule(packageName: String, windowChanged: Boolean = false) {
        pendingPackage = packageName
        timer.onEvent(windowChanged)?.let { delay ->
            handler.removeCallbacks(check)
            handler.postDelayed(check, delay)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(check)
        handler.removeCallbacks(tick)
        handler.removeCallbacks(hideNotice)
        hideNotice.run()
        timer.cancel()
        super.onDestroy()
    }

    private fun act(rule: AppRule, tab: AccessibilityNodeInfo? = null) {
        val now = SystemClock.elapsedRealtime()
        if (!timer.canAct(now)) return
        if (rule.packageName != lastActPackage || now - lastActAt >= ESCALATE_MS) backsInARow = 0
        // A tab rule only ever taps the tab: Back or Home would throw the user out of the app.
        val goHome = tab == null && escalate(actionOf(rule), backsInARow) == GateAction.HOME
        val performed = when {
            tab != null -> tab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            goHome -> performGlobalAction(GLOBAL_ACTION_HOME)
            else -> performGlobalAction(GLOBAL_ACTION_BACK)
        }
        if (performed) {
            timer.acted(now)
            // Home leaves the app on the cut screen, so reopening it starts with Back again.
            backsInARow = if (goHome) 0 else backsInARow + 1
            lastActPackage = rule.packageName
            lastActAt = now
            DayCount.bump(this)
            BlockLog.record(this, rule.packageName)
            // Look again once the debounce is over: Back does not always leave the screen.
            schedule(rule.packageName)
        }
    }

    /** The screen is open. Counts the time and says whether the screen has to go now. */
    private fun limitIsUsedUp(rule: AppRule): Boolean {
        if (rule.dailyMinutes == 0) return true
        val target = rule.packageName
        val limit = limitMs(rule)
        val before = UsageLog.today(this, target)
        if (before >= limit) {
            clock.stop()
            return true
        }
        // A locked phone can keep the app in the active window.
        if (getSystemService(PowerManager::class.java)?.isInteractive != true) {
            clock.stop()
            return false
        }
        if (clock.watching != target) BlockLog.seen(this, target)
        val step = clock.look(target, SystemClock.elapsedRealtime())
        if (step > 0) UsageLog.add(this, target, step)
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
        if (limitWarns(limit, before, before + step)) showNotice(getString(R.string.limit_warning, rule.label))
        return before + step >= limit
    }

    /** A toast would need the notification permission: Android drops it from a background app. */
    private fun showNotice(text: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.limit_notice, FrameLayout(this), false) as TextView
        view.text = text
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (NOTICE_BOTTOM_DP * resources.displayMetrics.density).toInt()
        }
        handler.removeCallbacks(hideNotice)
        hideNotice.run()
        getSystemService(WindowManager::class.java).addView(view, params)
        notice = view
        handler.postDelayed(hideNotice, NOTICE_MS)
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
            .filter { it.enabled && it.packageName !in PROTECTED_PACKAGES }
            .map { it.packageName }
            .toMutableSet()
        // Never empty: an empty list would mean every app.
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
        const val FOLLOW_UP_MS = 400L
        const val FOLLOW_UPS = 2
        const val RULES_CHECK_MS = 3000L
        const val ESCALATE_MS = 2500L
        const val TICK_MS = 1000L
        const val NOTICE_MS = 4000L
        const val NOTICE_BOTTOM_DP = 96
        const val MAX_NODES = 800
        const val TAB_CLASS = "android.app.ActionBar\$Tab"
    }
}
