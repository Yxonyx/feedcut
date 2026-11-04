package local.kapu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object RuleStore {
    fun file(context: Context): File = File(context.filesDir, "rules.json")

    fun read(context: Context): RuleSet {
        val target = file(context)
        if (!target.exists()) {
            context.assets.open("rules.json").use { input ->
                target.writeBytes(input.readBytes())
            }
        }
        return parse(target.readText())
    }

    fun readQuiet(context: Context): RuleSet = try {
        read(context)
    } catch (_: Exception) {
        RuleSet(emptyList())
    }

    fun writeRaw(context: Context, raw: String) {
        parse(raw)
        file(context).writeText(raw.trim() + "\n")
    }

    fun write(context: Context, rules: RuleSet) {
        writeRaw(context, toJson(rules))
    }

    fun reset(context: Context) {
        context.assets.open("rules.json").use { input ->
            file(context).writeBytes(input.readBytes())
        }
    }

    fun assetText(context: Context): String =
        context.assets.open("rules.json").bufferedReader().use { it.readText() }

    fun parse(raw: String): RuleSet {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("A JSON nem olvasható.")
        }
        val appsJson = root.optJSONArray("apps")
            ?: throw IllegalArgumentException("Hiányzik az apps lista.")
        val apps = ArrayList<AppRule>(appsJson.length())
        for (i in 0 until appsJson.length()) {
            val item = appsJson.optJSONObject(i)
                ?: throw IllegalArgumentException("Az apps[$i] nem objektum.")
            val packageName = item.optString("package").trim()
            if (packageName.isEmpty() || packageName.contains(' ')) {
                throw IllegalArgumentException("Hibás csomagnév az apps[$i] helyen.")
            }
            val action = item.optString("action", "back")
            if (action != "back" && action != "home") {
                throw IllegalArgumentException("Az action csak back vagy home lehet ($packageName).")
            }
            val hintsJson = item.optJSONArray("hints") ?: JSONArray()
            val hints = ArrayList<String>(hintsJson.length())
            for (h in 0 until hintsJson.length()) {
                val hint = hintsJson.optString(h).trim()
                if (hint.isNotEmpty()) hints.add(hint)
            }
            val allowJson = item.optJSONArray("allowTabs") ?: JSONArray()
            val allowTabs = ArrayList<String>(allowJson.length())
            for (a in 0 until allowJson.length()) {
                val tab = allowJson.optString(a).trim()
                if (tab.isNotEmpty()) allowTabs.add(tab)
            }
            apps.add(
                AppRule(
                    packageName = packageName,
                    label = item.optString("label").ifBlank { packageName },
                    enabled = item.optBoolean("enabled", false),
                    action = action,
                    hints = hints,
                    allowTabs = allowTabs,
                )
            )
        }
        return RuleSet(apps)
    }

    fun toJson(rules: RuleSet): String {
        val apps = JSONArray()
        for (rule in rules.apps) {
            val hints = JSONArray()
            rule.hints.forEach { hints.put(it) }
            val item = JSONObject()
                .put("package", rule.packageName)
                .put("label", rule.label)
                .put("enabled", rule.enabled)
                .put("action", rule.action)
                .put("hints", hints)
            if (rule.allowTabs.isNotEmpty()) {
                item.put("allowTabs", JSONArray(rule.allowTabs))
            }
            apps.put(item)
        }
        return JSONObject().put("apps", apps).toString(2) + "\n"
    }
}
