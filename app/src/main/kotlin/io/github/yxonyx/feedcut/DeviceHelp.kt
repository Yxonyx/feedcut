package io.github.yxonyx.feedcut

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Vendor-specific settings screens. They are hidden and change between versions,
 * so App info is always the fallback.
 */
object DeviceHelp {
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${activity.packageName}".toUri(),
        )
        activity.startActivity(intent)
    }

    fun openBattery(activity: Activity) {
        if (isXiaomi) {
            val intent = Intent()
                .setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                .putExtra("package_name", activity.packageName)
                .putExtra("package_label", activity.getString(R.string.app_name))
            if (tryStart(activity, intent)) return
        }
        openAppSettings(activity)
    }

    fun openAutostart(activity: Activity) {
        val intent = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        if (!tryStart(activity, intent)) openAppSettings(activity)
    }

    fun openDontKillMyApp(activity: Activity) {
        val maker = Build.MANUFACTURER.lowercase().replace(' ', '-')
        tryStart(activity, Intent(Intent.ACTION_VIEW, "https://dontkillmyapp.com/$maker".toUri()))
    }

    private fun tryStart(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
