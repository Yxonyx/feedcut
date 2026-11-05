package io.github.yxonyx.feedcut

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** The raw rules file. Saves only what parses; Back asks before dropping an edit. */
class RulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)
        fitScreen()
        val editor = findViewById<EditText>(R.id.editor)
        val original = RuleStore.text(this)
        if (savedInstanceState == null) editor.setText(original)

        findViewById<View>(R.id.save).setOnClickListener {
            try {
                if (saveRules { RuleStore.writeRaw(this, editor.text.toString()) }) finish()
            } catch (e: RuleError) {
                Toast.makeText(this, getString(e.res, *e.args), Toast.LENGTH_LONG).show()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editor.text.toString() == original) {
                    finish()
                    return
                }
                MaterialAlertDialogBuilder(this@RulesActivity)
                    .setMessage(R.string.discard_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.discard) { _, _ -> finish() }
                    .show()
            }
        })
    }
}
