package io.github.yxonyx.kapu

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object GateStatus {
    fun isEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${GateService::class.java.name}"
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(flat)
        for (part in splitter) {
            if (part.equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
