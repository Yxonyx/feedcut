package local.kapu

import android.content.Context
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Hibás szabályfájl. Az üzenet erőforrásból jön, hogy minden nyelven olvasható legyen. */
class RuleError(@StringRes val res: Int, vararg val args: Any) : IllegalArgumentException()

object RuleStore {
    fun file(context: Context): File = File(context.filesDir, "rules.json")

    /** A gyári szabályok másolata, amiből a felhasználó fájlja indult. Az összefésüléshez kell. */
    private fun baseFile(context: Context): File = File(context.filesDir, "rules.base.json")

    fun read(context: Context): RuleSet {
        val target = file(context)
        if (!target.exists()) {
            reset(context)
            return parse(target.readText())
        }
        val user = parse(target.readText())
        val bundled = parse(assetText(context))
        if (bundled.version <= user.version) return user
        // Az app frissült, és újabb gyári jeleket hozott.
        val base = try {
            baseFile(context).takeIf { it.exists() }?.let { parse(it.readText()) }
        } catch (_: IllegalArgumentException) {
            null
        }
        val merged = mergeRules(base, user, bundled)
        target.writeText(toJson(merged))
        baseFile(context).writeText(assetText(context))
        return merged
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
        val raw = assetText(context)
        file(context).writeText(raw)
        baseFile(context).writeText(raw)
    }

    fun assetText(context: Context): String =
        context.assets.open("rules.json").bufferedReader().use { it.readText() }

    fun parse(raw: String): RuleSet {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw RuleError(R.string.err_json)
        }
        val appsJson = root.optJSONArray("apps") ?: throw RuleError(R.string.err_no_apps)
        val apps = ArrayList<AppRule>(appsJson.length())
        for (i in 0 until appsJson.length()) {
            val item = appsJson.optJSONObject(i) ?: throw RuleError(R.string.err_not_object, i)
            val packageName = item.optString("package").trim()
            if (packageName.isEmpty() || packageName.contains(' ')) {
                throw RuleError(R.string.err_package, i)
            }
            val action = item.optString("action", "back")
            if (action != "back" && action != "home") {
                throw RuleError(R.string.err_action, packageName)
            }
            apps.add(
                AppRule(
                    packageName = packageName,
                    label = item.optString("label").ifBlank { packageName },
                    enabled = item.optBoolean("enabled", false),
                    action = action,
                    hints = strings(item.optJSONArray("hints")),
                    allowTabs = strings(item.optJSONArray("allowTabs")),
                )
            )
        }
        return RuleSet(apps, root.optInt("version", 0))
    }

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i).trim()
            if (value.isNotEmpty()) out.add(value)
        }
        return out
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
        return JSONObject().put("version", rules.version).put("apps", apps).toString(2) + "\n"
    }
}
