package io.github.yxonyx.kapu

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
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

    /**
     * Whether Android is actually running the service. A service that was killed can stay
     * switched on in Settings without being bound again; then only toggling it helps.
     */
    fun isRunning(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val expected = "${context.packageName}/${GateService::class.java.name}"
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id.equals(expected, ignoreCase = true) }
    }
}
