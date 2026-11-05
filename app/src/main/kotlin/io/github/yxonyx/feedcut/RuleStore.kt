package io.github.yxonyx.feedcut

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** A bad rules file. The message is a string resource, so it is translated. */
class RuleError(@StringRes val res: Int, vararg val args: Any) : IllegalArgumentException()

object RuleStore {
    fun file(context: Context): File = File(context.filesDir, "rules.json")

    /** Copy of the default rules the user's file started from. Needed for the merge. */
    private fun baseFile(context: Context): File = File(context.filesDir, "rules.base.json")

    fun read(context: Context): RuleSet {
        val target = file(context)
        if (!target.exists()) {
            reset(context)
            return parse(load(target))
        }
        val user = parse(load(target))
        val bundled = parse(assetText(context))
        if (bundled.version <= user.version) return user
        // The app was updated and brought newer default rules.
        // The base only counts if it is the defaults this file was last merged from. A crash
        // between the two saves below leaves them on different versions.
        val base = try {
            baseFile(context).takeIf { it.exists() }?.let { parse(load(it)) }
                ?.takeIf { it.version == user.version }
        } catch (_: IllegalArgumentException) {
            null
        }
        val merged = mergeRules(base, user, bundled)
        save(target, toJson(merged))
        save(baseFile(context), assetText(context))
        return merged
    }

    /** For screens: a damaged file is set aside and the defaults load, and [onRecovered] says so. */
    fun readForUi(context: Context, onRecovered: () -> Unit): RuleSet = try {
        read(context)
    } catch (_: IllegalArgumentException) {
        recover(context)
        onRecovered()
        read(context)
    } catch (_: IOException) {
        // Usually a full disk. Show the defaults rather than nothing; the service keeps its copy.
        parse(assetText(context))
    }

    /** For the service: null when the file cannot be read right now. */
    fun readQuiet(context: Context): RuleSet? = try {
        read(context)
    } catch (e: Exception) {
        Log.w("Feedcut", "rules file not readable", e)
        null
    }

    fun writeRaw(context: Context, raw: String) {
        parse(raw)
        save(file(context), raw.trim() + "\n")
    }

    fun write(context: Context, rules: RuleSet) {
        writeRaw(context, toJson(rules))
    }

    fun reset(context: Context) {
        val raw = assetText(context)
        save(file(context), raw)
        save(baseFile(context), raw)
    }

    /**
     * Keeps the damaged file as rules.broken.json and restores the default rules,
     * so the user's own hints are not lost without a trace.
     */
    fun recover(context: Context) {
        val target = file(context)
        if (target.exists()) target.copyTo(File(context.filesDir, "rules.broken.json"), overwrite = true)
        reset(context)
    }

    /** Atomic write: an interrupted save never leaves a half-written file. */
    private fun save(target: File, text: String) {
        val atomic = AtomicFile(target)
        val out = atomic.startWrite()
        try {
            out.write(text.toByteArray())
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
            throw e
        }
    }

    private fun load(target: File): String = String(AtomicFile(target).readFully())

    /** For the editor: the saved file, or the defaults if there is none yet. */
    fun text(context: Context): String {
        val target = file(context)
        return if (target.exists()) load(target) else assetText(context)
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
                    note = item.optString("note").trim(),
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
            if (rule.note.isNotEmpty()) item.put("note", rule.note)
            apps.put(item)
        }
        return JSONObject().put("version", rules.version).put("apps", apps).toString(2) + "\n"
    }
}
