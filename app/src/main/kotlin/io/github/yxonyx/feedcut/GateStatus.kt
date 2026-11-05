package io.github.yxonyx.feedcut

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object GateStatus {
    private fun self(context: Context) = ComponentName(context, GateService::class.java)

    /**
     * Android stores service names in full or short form ("pkg/.GateService"), so compare
     * parsed component names, never the strings.
     */
    private fun isSelf(context: Context, flat: String): Boolean =
        ComponentName.unflattenFromString(flat) == self(context)

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return flat.split(':').any { isSelf(context, it) }
    }

    /**
     * Whether Android is actually running the service. A service that was killed can stay
     * switched on in Settings without being bound again; then only toggling it helps.
     */
    fun isRunning(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { isSelf(context, it.id) }
    }

    fun state(context: Context): GateState = gateState(isEnabled(context), isRunning(context))

    /** Tells the running service to reload the rules file now. */
    fun ping(context: Context) {
        if (!isEnabled(context)) return
        // The service also rereads the file every few seconds, so a refused start only costs time.
        try {
            context.startService(Intent(context, GateService::class.java))
        } catch (_: IllegalStateException) {
        }
    }
}
