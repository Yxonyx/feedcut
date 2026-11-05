package io.github.yxonyx.feedcut

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/** Time spent today on screens with a daily limit, per app. Stays on the phone. */
object UsageLog {
    private const val PREFS = "feedcut_usage"

    fun add(context: Context, packageName: String, ms: Long) {
        val prefs = prefs(context)
        val today = LocalDate.now().toString()
        val key = key(today, packageName)
        prefs.edit {
            putLong(key, prefs.getLong(key, 0L) + ms)
            prefs.all.keys.filterNot { it.startsWith("$today/") }.forEach { remove(it) }
        }
    }

    fun today(context: Context, packageName: String): Long =
        prefs(context).getLong(key(LocalDate.now().toString(), packageName), 0L)

    private fun key(day: String, packageName: String) = "$day/$packageName"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
