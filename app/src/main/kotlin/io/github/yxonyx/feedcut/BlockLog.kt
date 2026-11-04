package io.github.yxonyx.feedcut

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat

/** Per app: when Feedcut last closed a screen, and the app's version at that time. Stays on the phone. */
object BlockLog {
    private const val PREFS = "feedcut_blocks"

    fun record(context: Context, packageName: String) {
        val version = installedVersion(context, packageName)
        prefs(context).edit {
            putLong("$packageName.at", System.currentTimeMillis())
            if (version != null) putLong("$packageName.version", version)
        }
    }

    fun lastAt(context: Context, packageName: String): Long? =
        prefs(context).getLong("$packageName.at", -1L).takeIf { it >= 0 }

    fun versionAtBlock(context: Context, packageName: String): Long? =
        prefs(context).getLong("$packageName.version", -1L).takeIf { it >= 0 }

    fun installedVersion(context: Context, packageName: String): Long? = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(packageName, 0))
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun health(context: Context, packageName: String): RuleHealth = ruleHealth(
        installedVersion(context, packageName),
        lastAt(context, packageName),
        versionAtBlock(context, packageName),
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
