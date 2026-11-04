package io.github.yxonyx.feedcut

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

/** Screens closed per day, kept for a month on the phone. */
object DayCount {
    private const val PREFS = "feedcut_count"
    private const val KEEP_DAYS = 31L

    fun bump(context: Context) {
        val today = LocalDate.now()
        val prefs = prefs(context)
        val key = today.toString()
        prefs.edit {
            putInt(key, prefs.getInt(key, 0) + 1)
            prefs.all.keys
                .filter { it < today.minusDays(KEEP_DAYS).toString() }
                .forEach { remove(it) }
        }
    }

    fun today(context: Context): Int = prefs(context).getInt(LocalDate.now().toString(), 0)

    /** The last [days] days, oldest first, ending today. */
    fun lastDays(context: Context, days: Int): List<Pair<LocalDate, Int>> {
        val prefs = prefs(context)
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            day to prefs.getInt(day.toString(), 0)
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
