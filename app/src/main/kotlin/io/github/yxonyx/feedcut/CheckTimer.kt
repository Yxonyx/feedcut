package io.github.yxonyx.feedcut

/**
 * When to run the next check. No Android calls, so it can be unit tested.
 *
 * A playing video sends many contentChanged events per second. They are merged into
 * one check. A window change is checked at once. After an action nothing is decided
 * until the debounce ends, but then the screen is checked again, otherwise a quick
 * way back into the blocked screen would be missed.
 *
 * A window that is still opening can report its views as not visible, and a still screen
 * sends no more events. So after a window change that showed nothing, the screen is
 * checked again a few times on its own.
 */
class CheckTimer(
    private val settleMs: Long,
    private val debounceMs: Long,
    private val followUpMs: Long = 0,
    private val followUps: Int = 0,
) {
    var pending = false
        private set
    private var lastActionAt = -debounceMs
    private var followUpsLeft = 0

    /** An event arrived. Returns the delay if a check has to be (re)scheduled. */
    fun onEvent(windowChanged: Boolean): Long? {
        if (windowChanged) followUpsLeft = followUps
        if (!windowChanged && pending) return null
        pending = true
        return if (windowChanged) 0L else settleMs
    }

    /** The scheduled check fired. While the debounce lasts, returns how long to wait before checking again. */
    fun onFire(now: Long): Long? {
        val wait = debounceMs - (now - lastActionAt)
        if (wait > 0) return wait
        pending = false
        return null
    }

    /** A check found nothing to do. Returns the delay of a follow-up check, if one is left. */
    fun foundNothing(): Long? {
        if (pending || followUpsLeft <= 0) return null
        followUpsLeft--
        pending = true
        return followUpMs
    }

    fun canAct(now: Long): Boolean = now - lastActionAt >= debounceMs

    fun acted(now: Long) {
        lastActionAt = now
    }

    fun cancel() {
        pending = false
    }
}
