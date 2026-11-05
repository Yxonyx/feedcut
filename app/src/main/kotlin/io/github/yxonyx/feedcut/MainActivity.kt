package io.github.yxonyx.feedcut

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    private var shown: RuleSet = RuleSet(emptyList())
    private var writing = false

    // One filled button on the screen: the next step to take.
    private var primaryUsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitScreen()
        findViewById<View>(R.id.advanced).setOnClickListener {
            startActivity(Intent(this, AdvancedActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        shown = RuleStore.readForUi(this) {
            Toast.makeText(this, R.string.rules_recovered, Toast.LENGTH_LONG).show()
        }
        renderStatus()
        renderToday()
        renderSetup()
        renderApps()
    }

    private fun renderStatus() {
        val state = GateStatus.state(this)
        val watching = shown.apps.count { it.enabled && BlockLog.installedVersion(this, it.packageName) != null }
        findViewById<BlazeView>(R.id.blaze).state = state
        findViewById<TextView>(R.id.status).text = when (state) {
            GateState.ON -> resources.getQuantityString(R.plurals.status_on, watching, watching)
            GateState.OFF -> getString(R.string.status_off)
            GateState.STUCK -> getString(R.string.status_stuck)
        }
    }

    private fun renderToday() {
        val cuts = DayCount.today(this)
        findViewById<TextView>(R.id.count).text = java.text.NumberFormat.getIntegerInstance().format(cuts)
        findViewById<TextView>(R.id.count_label).text = resources.getQuantityString(R.plurals.count_label, cuts)
        findViewById<TextView>(R.id.kept).text = getString(R.string.kept, minutesKept(cuts))
        val week = DayCount.lastDays(this, 7)
        val empty = week.all { (_, cuts) -> cuts == 0 }
        findViewById<WeekView>(R.id.week).apply {
            show(week)
            visibility = if (empty) View.GONE else View.VISIBLE
        }
        findViewById<View>(R.id.week_empty).visibility = if (empty) View.VISIBLE else View.GONE
    }

    /** Setup shows as waymarks only until the steps Feedcut can check are done. */
    private fun renderSetup() {
        val state = GateStatus.state(this)
        val accessOk = state == GateState.ON
        val batteryOk = getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
        val done = listOf(accessOk, batteryOk).count { it }
        findViewById<TextView>(R.id.setup_state).text =
            if (done == 2) getString(R.string.setup_done) else getString(R.string.setup_progress, done, 2)
        val block = findViewById<View>(R.id.setup_block)
        block.visibility = if (done == 2) View.GONE else View.VISIBLE
        if (done == 2) return
        val steps = findViewById<LinearLayout>(R.id.setup_steps)
        steps.removeAllViews()
        primaryUsed = false
        step(
            steps,
            R.string.step_access,
            done = accessOk,
            detail = when (state) {
                GateState.OFF -> getString(R.string.step_access_off)
                GateState.STUCK -> getString(R.string.step_access_stuck)
                GateState.ON -> null
            },
            actions = if (accessOk) emptyList() else listOf(
                R.string.open_access to { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                R.string.open_settings to { DeviceHelp.openAppSettings(this) },
            ),
        )
        step(
            steps,
            R.string.step_battery,
            done = batteryOk,
            detail = if (batteryOk) null else getString(
                if (DeviceHelp.isXiaomi) R.string.step_battery_xiaomi else R.string.step_battery_todo,
            ),
            actions = if (batteryOk) emptyList() else listOf(R.string.open_battery to { DeviceHelp.openBattery(this) }),
        )
        if (DeviceHelp.isXiaomi) {
            step(
                steps,
                R.string.step_autostart,
                done = false,
                detail = getString(R.string.step_autostart_detail),
                actions = listOf(R.string.open_autostart to { DeviceHelp.openAutostart(this) }),
            )
        }
    }

    private fun step(
        parent: LinearLayout,
        title: Int,
        done: Boolean,
        detail: String?,
        actions: List<Pair<Int, () -> Unit>>,
    ) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_setup_step, parent, false)
        row.findViewById<View>(R.id.mark).setBackgroundResource(if (done) R.drawable.waymark_done else R.drawable.waymark_todo)
        row.findViewById<TextView>(R.id.title).setText(title)
        row.findViewById<TextView>(R.id.detail).apply {
            text = detail
            visibility = if (detail == null) View.GONE else View.VISIBLE
        }
        val bar = row.findViewById<LinearLayout>(R.id.actions)
        bar.visibility = if (actions.isEmpty()) View.GONE else View.VISIBLE
        actions.forEachIndexed { i, (label, run) ->
            val primary = i == 0 && !primaryUsed
            if (primary) primaryUsed = true
            val style = if (primary) com.google.android.material.R.attr.materialButtonStyle
            else com.google.android.material.R.attr.materialButtonOutlinedStyle
            val button = MaterialButton(this, null, style)
            button.setText(label)
            button.maxLines = 1
            button.setOnClickListener { run() }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            if (i > 0) params.topMargin = (4 * resources.displayMetrics.density).toInt()
            bar.addView(button, params)
        }
        parent.addView(row)
    }

    private fun renderApps() {
        writing = true
        val list = findViewById<LinearLayout>(R.id.app_list)
        list.removeAllViews()
        val inflater = LayoutInflater.from(this)
        // Installed apps first; the index still points into the rules file.
        val rows = shown.apps.withIndex()
            .map { (index, rule) -> Triple(index, rule, BlockLog.health(this, rule.packageName)) }
            .sortedBy { (_, _, health) -> health == RuleHealth.NOT_INSTALLED }
        rows.forEach { (index, rule, health) ->
            val row = inflater.inflate(R.layout.item_app_row, list, false)
            val label = row.findViewById<TextView>(R.id.label)
            label.text = rule.label
            val detail = row.findViewById<TextView>(R.id.detail)
            detail.text = detailLine(rule, health)
            if (rule.enabled && health == RuleHealth.APP_UPDATED) {
                detail.setTextColor(ContextCompat.getColor(this, R.color.c_warn))
            }
            val installed = health != RuleHealth.NOT_INSTALLED
            if (!installed) label.setTextColor(ContextCompat.getColor(this, R.color.c_muted))
            val sw = row.findViewById<MaterialSwitch>(R.id.enabled)
            // Nothing to switch for an app that is not there.
            sw.visibility = if (installed) View.VISIBLE else View.GONE
            sw.isChecked = rule.enabled
            sw.contentDescription = rule.label
            sw.setOnCheckedChangeListener { _, checked ->
                if (!writing) update(index) { it.copy(enabled = checked) }
            }
            list.addView(row)
        }
        writing = false
    }

    /** Fixed furniture for every app: what it removes, then its state. */
    private fun detailLine(rule: AppRule, health: RuleHealth): String {
        val state = when {
            health == RuleHealth.NOT_INSTALLED -> getString(R.string.not_installed)
            !rule.enabled -> getString(R.string.detail_off)
            health == RuleHealth.APP_UPDATED -> getString(R.string.health_updated)
            health == RuleHealth.NEVER_BLOCKED -> getString(R.string.health_never)
            else -> lastCut(BlockLog.lastAt(this, rule.packageName) ?: 0L)
        }
        return if (rule.note.isEmpty()) state else "${rule.note} · $state"
    }

    /** "last cut 5 minutes ago", but "last cut just now" instead of "0 minutes ago". */
    private fun lastCut(at: Long): String {
        val now = System.currentTimeMillis()
        if (now - at < DateUtils.MINUTE_IN_MILLIS) return getString(R.string.health_just_now)
        return getString(
            R.string.health_last,
            DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS),
        )
    }

    private fun update(index: Int, change: (AppRule) -> AppRule) {
        val apps = shown.apps.toMutableList()
        if (index !in apps.indices) return
        apps[index] = change(apps[index])
        val next = shown.copy(apps = apps)
        if (saveRules { RuleStore.write(this, next) }) shown = next
        renderApps()
        renderStatus()
    }
}
