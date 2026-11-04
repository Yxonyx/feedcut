package io.github.yxonyx.kapu

/**
 * When to run the next check. No Android calls, so it can be unit tested.
 *
 * A playing video sends many contentChanged events per second. They are merged into
 * one check. A window change is checked at once. After an action nothing is decided
 * until the debounce ends, but then the screen is checked again, otherwise a quick
 * way back into the blocked screen would be missed.
 */
class CheckTimer(
    private val settleMs: Long,
    private val debounceMs: Long,
) {
    var pending = false
        private set
    private var lastActionAt = -debounceMs

    /** An event arrived. Returns the delay if a check has to be (re)scheduled. */
    fun onEvent(windowChanged: Boolean): Long? {
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

    fun canAct(now: Long): Boolean = now - lastActionAt >= debounceMs

    fun acted(now: Long) {
        lastActionAt = now
    }

    fun cancel() {
        pending = false
    }
}
