package io.github.yxonyx.feedcut

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

/** Screen markers, the Back/Home choice, the raw rules file. Kept off the main screen. */
class AdvancedActivity : AppCompatActivity() {
    private var shown: RuleSet = RuleSet(emptyList())
    private var writing = false

    // The hints field does not save on every keystroke, only after half a second of quiet.
    private val saver = Handler(Looper.getMainLooper())
    private val pendingHints = LinkedHashMap<Int, Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced)
        fitScreen()
        findViewById<View>(R.id.edit_json).setOnClickListener {
            startActivity(Intent(this, RulesActivity::class.java))
        }
        findViewById<View>(R.id.reset_rules).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.reset_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reset_rules) { _, _ ->
                    flushHints()
                    saveRules { RuleStore.reset(this) }
                    render()
                }
                .show()
        }
        findViewById<View>(R.id.open_dontkill).setOnClickListener { DeviceHelp.openDontKillMyApp(this) }
        if (DeviceHelp.isXiaomi) findViewById<View>(R.id.xiaomi_note).visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onPause() {
        flushHints()
        super.onPause()
    }

    private fun flushHints() {
        val waiting = pendingHints.values.toList()
        pendingHints.clear()
        waiting.forEach {
            saver.removeCallbacks(it)
            it.run()
        }
    }

    private fun render() {
        writing = true
        shown = RuleStore.readForUi(this) {
            Toast.makeText(this, R.string.rules_recovered, Toast.LENGTH_LONG).show()
        }
        val list = findViewById<LinearLayout>(R.id.app_list)
        list.removeAllViews()
        val inflater = LayoutInflater.from(this)
        shown.apps.forEachIndexed { index, rule ->
            val row = inflater.inflate(R.layout.item_rule_edit, list, false)
            row.findViewById<TextView>(R.id.label).text = rule.label
            row.findViewById<TextView>(R.id.pkg).text = rule.packageName
            // The label of the field stays in the body face; only the ids are monospace.
            row.findViewById<TextInputLayout>(R.id.hints_box).typeface = Typeface.DEFAULT
            if (rule.allowTabs.isNotEmpty()) {
                // A tab rule only taps a tab, so Back/Home and markers do not apply.
                row.findViewById<View>(R.id.action).visibility = View.GONE
                row.findViewById<View>(R.id.hints_box).visibility = View.GONE
                row.findViewById<TextView>(R.id.tabs).apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.tabs_rule, rule.allowTabs.joinToString(", "))
                }
                list.addView(row)
                return@forEachIndexed
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
                        .split(',', '\n')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    pendingHints.remove(index)?.let(saver::removeCallbacks)
                    val save = Runnable {
                        pendingHints.remove(index)
                        update(index) { it.copy(hints = parts) }
                    }
                    pendingHints[index] = save
                    saver.postDelayed(save, HINT_SAVE_MS)
                }
            })
            list.addView(row)
        }
        findViewById<TextView>(R.id.version).text =
            getString(R.string.version_line, BuildConfig.VERSION_NAME, shown.version)
        writing = false
    }

    private fun update(index: Int, change: (AppRule) -> AppRule) {
        val apps = shown.apps.toMutableList()
        if (index !in apps.indices) return
        apps[index] = change(apps[index])
        val next = shown.copy(apps = apps)
        if (saveRules { RuleStore.write(this, next) }) shown = next
    }

    private companion object {
        const val HINT_SAVE_MS = 500L
    }
}
