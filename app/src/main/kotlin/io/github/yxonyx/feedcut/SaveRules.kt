package io.github.yxonyx.feedcut

import android.content.Context
import android.widget.Toast
import java.io.IOException

/**
 * Runs a rules write from a screen. On success the service rereads the file; on a failed
 * write (usually a full disk) the user is told and nothing crashes. Returns whether it saved.
 */
internal fun Context.saveRules(write: () -> Unit): Boolean = try {
    write()
    GateStatus.ping(this)
    true
} catch (_: IOException) {
    Toast.makeText(this, R.string.err_save, Toast.LENGTH_LONG).show()
    false
}
