package io.github.yxonyx.feedcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenClockTest {
    private fun clock() = ScreenClock(stepMs = 1_000, maxStepMs = 2_000)

    @Test
    fun firstLookCountsNothing() {
        val c = clock()
        assertEquals(0L, c.look("ig", 5_000))
        assertEquals("ig", c.watching)
    }

    @Test
    fun timeAddsUpInSteps() {
        val c = clock()
        c.look("ig", 5_000)
        assertEquals(0L, c.look("ig", 5_400))
        assertEquals(1_200L, c.look("ig", 6_200))
        assertEquals(1_000L, c.look("ig", 7_200))
    }

    @Test
    fun longGapCountsOnlyOneStep() {
        val c = clock()
        c.look("ig", 5_000)
        assertEquals(2_000L, c.look("ig", 300_000))
    }

    @Test
    fun otherAppStartsAgain() {
        val c = clock()
        c.look("ig", 5_000)
        assertEquals(0L, c.look("yt", 6_500))
        assertEquals("yt", c.watching)
    }

    @Test
    fun stopForgetsTheScreen() {
        val c = clock()
        c.look("ig", 5_000)
        c.stop()
        assertNull(c.watching)
        assertEquals(0L, c.look("ig", 9_000))
    }
}
