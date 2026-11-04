package io.github.yxonyx.kapu

/**
 * Mikor fusson a következő ellenőrzés. Nincs benne Android-hívás, hogy tesztelhető legyen.
 *
 * Egy videó másodpercenként sok contentChanged eseményt küld. Ezeket egy ellenőrzéssé
 * vonjuk össze. Ablakváltásnál azonnal nézünk. Egy lépés után a védőidő végéig nem
 * döntünk, de akkor újranézünk, különben egy gyors visszalépés elveszne.
 */
class CheckTimer(
    private val settleMs: Long,
    private val debounceMs: Long,
) {
    var pending = false
        private set
    private var lastActionAt = -debounceMs

    /** Esemény jött. Ha (újra) ütemezni kell, a késleltetést adja vissza. */
    fun onEvent(windowChanged: Boolean): Long? {
        if (!windowChanged && pending) return null
        pending = true
        return if (windowChanged) 0L else settleMs
    }

    /** Lefutott az ütemezett ellenőrzés. Ha a védőidő még tart, ennyi múlva kell újranézni. */
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
