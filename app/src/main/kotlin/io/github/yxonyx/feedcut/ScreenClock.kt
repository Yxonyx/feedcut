package io.github.yxonyx.feedcut

/**
 * How long a limited screen stays open. No Android calls, so it can be unit tested.
 *
 * A gap counts as one short step at most: the system may have frozen the service,
 * and that time must not eat the user's limit.
 */
class ScreenClock(
    private val stepMs: Long,
    private val maxStepMs: Long,
) {
    var watching: String? = null
        private set
    private var countedTo = 0L

    /** The screen is open now. Returns the time to add to today's total. */
    fun look(packageName: String, now: Long): Long {
        if (packageName != watching) {
            watching = packageName
            countedTo = now
            return 0
        }
        val passed = now - countedTo
        if (passed < stepMs) return 0
        countedTo = now
        return minOf(passed, maxStepMs)
    }

    fun stop() {
        watching = null
    }
}
