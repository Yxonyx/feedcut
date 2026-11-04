package io.github.yxonyx.kapu

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Gyártófüggő beállítóképernyők. A gyártói képernyők rejtettek és változnak,
 * ezért mindegyiknél az App infó a tartalék.
 */
object DeviceHelp {
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity.packageName}"),
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
        tryStart(activity, Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/$maker")))
    }

    /** A manifest <queries> blokkja miatt az indítható appok láthatók, a többi nem. */
    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    private fun tryStart(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
