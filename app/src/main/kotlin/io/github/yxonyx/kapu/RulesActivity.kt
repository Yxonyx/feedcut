package io.github.yxonyx.kapu

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)
        fitScreen()
        val editor = findViewById<EditText>(R.id.editor)
        val file = RuleStore.file(this)
        editor.setText(if (file.exists()) file.readText() else RuleStore.assetText(this))
        findViewById<android.view.View>(R.id.save).setOnClickListener {
            try {
                RuleStore.writeRaw(this, editor.text.toString())
                if (GateStatus.isEnabled(this)) {
                    startService(Intent(this, GateService::class.java))
                }
                finish()
            } catch (e: RuleError) {
                Toast.makeText(this, getString(e.res, *e.args), Toast.LENGTH_LONG).show()
            }
        }
    }
}
