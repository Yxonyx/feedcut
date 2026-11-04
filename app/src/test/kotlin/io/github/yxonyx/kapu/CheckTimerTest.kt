package io.github.yxonyx.kapu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckTimerTest {
    private fun timer() = CheckTimer(settleMs = 120, debounceMs = 800)

    @Test
    fun contentEventsCoalesce() {
        val t = timer()
        assertEquals(120L, t.onEvent(windowChanged = false))
        assertNull(t.onEvent(windowChanged = false))
        assertTrue(t.pending)
    }

    @Test
    fun windowChangeChecksAtOnceEvenWhilePending() {
        val t = timer()
        t.onEvent(windowChanged = false)
        assertEquals(0L, t.onEvent(windowChanged = true))
    }

    @Test
    fun fireAfterActionWaitsOutTheDebounce() {
        val t = timer()
        t.acted(now = 10_000)
        t.onEvent(windowChanged = true)
        assertEquals(500L, t.onFire(now = 10_300))
        assertTrue(t.pending)
        assertNull(t.onFire(now = 10_800))
        assertFalse(t.pending)
    }

    @Test
    fun canActRespectsDebounce() {
        val t = timer()
        assertTrue(t.canAct(now = 0))
        t.acted(now = 1_000)
        assertFalse(t.canAct(now = 1_799))
        assertTrue(t.canAct(now = 1_800))
    }

    @Test
    fun cancelClearsPending() {
        val t = timer()
        t.onEvent(windowChanged = false)
        t.cancel()
        assertEquals(120L, t.onEvent(windowChanged = false))
    }
}
