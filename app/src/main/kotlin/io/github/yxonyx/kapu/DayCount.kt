package io.github.yxonyx.kapu

import android.content.Context
import java.time.LocalDate

object DayCount {
    private const val PREFS = "kapu_count"
    private const val DAY = "day"
    private const val N = "n"

    fun bump(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val next = if (prefs.getString(DAY, "") == today) prefs.getInt(N, 0) + 1 else 1
        prefs.edit().putString(DAY, today).putInt(N, next).apply()
    }

    fun today(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(DAY, "") != LocalDate.now().toString()) return 0
        return prefs.getInt(N, 0)
    }
}
