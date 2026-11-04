package io.github.yxonyx.kapu

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    private var shown: RuleSet = RuleSet(emptyList())
    private var writing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitScreen()
        findViewById<android.view.View>(R.id.open_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<android.view.View>(R.id.open_settings).setOnClickListener {
            DeviceHelp.openAppSettings(this)
        }
        findViewById<android.view.View>(R.id.open_battery).setOnClickListener {
            DeviceHelp.openBattery(this)
        }
        findViewById<android.view.View>(R.id.open_dontkill).setOnClickListener {
            DeviceHelp.openDontKillMyApp(this)
        }
        if (DeviceHelp.isXiaomi) {
            findViewById<TextView>(R.id.battery_hint).setText(R.string.battery_hint_xiaomi)
            findViewById<android.view.View>(R.id.open_autostart).apply {
                visibility = android.view.View.VISIBLE
                setOnClickListener { DeviceHelp.openAutostart(this@MainActivity) }
            }
        }
        findViewById<android.view.View>(R.id.edit_json).setOnClickListener {
            startActivity(Intent(this, RulesActivity::class.java))
        }
        findViewById<android.view.View>(R.id.reset_rules).setOnClickListener {
            RuleStore.reset(this)
            ping()
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        val status = findViewById<TextView>(R.id.status)
        status.setText(if (GateStatus.isEnabled(this)) R.string.status_on else R.string.status_off)
        findViewById<TextView>(R.id.today).text =
            getString(R.string.today_count, DayCount.today(this))
        render()
    }

    private fun render() {
        writing = true
        shown = try {
            RuleStore.read(this)
        } catch (_: Exception) {
            RuleStore.reset(this)
            RuleStore.read(this)
        }
        val list = findViewById<LinearLayout>(R.id.app_list)
        list.removeAllViews()
        val inflater = LayoutInflater.from(this)
        shown.apps.forEachIndexed { index, rule ->
            val row = inflater.inflate(R.layout.item_app, list, false)
            row.findViewById<TextView>(R.id.label).text = rule.label
            val installed = DeviceHelp.isInstalled(this, rule.packageName)
            row.findViewById<TextView>(R.id.pkg).text =
                if (installed) rule.packageName else "${rule.packageName} · ${getString(R.string.not_installed)}"
            if (!installed) row.alpha = 0.5f
            val sw = row.findViewById<MaterialSwitch>(R.id.enabled)
            sw.isChecked = rule.enabled
            sw.setOnCheckedChangeListener { _, checked ->
                if (!writing) update(index) { it.copy(enabled = checked) }
            }
            val group = row.findViewById<MaterialButtonToggleGroup>(R.id.action)
            group.check(if (rule.action == "home") R.id.action_home else R.id.action_back)
            group.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (writing || !isChecked) return@addOnButtonCheckedListener
                val action = if (checkedId == R.id.action_home) "home" else "back"
                update(index) { it.copy(action = action) }
            }
            val hints = row.findViewById<EditText>(R.id.hints)
            hints.setText(rule.hints.joinToString(", "))
            hints.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (writing) return
                    val parts = s?.toString().orEmpty()
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    update(index) { it.copy(hints = parts) }
                }
            })
            list.addView(row)
        }
        writing = false
    }

    private fun update(index: Int, change: (AppRule) -> AppRule) {
        val apps = shown.apps.toMutableList()
        if (index !in apps.indices) return
        apps[index] = change(apps[index])
        shown = RuleSet(apps)
        RuleStore.write(this, shown)
        ping()
    }

    private fun ping() {
        if (!GateStatus.isEnabled(this)) return
        startService(Intent(this, GateService::class.java))
    }
}
