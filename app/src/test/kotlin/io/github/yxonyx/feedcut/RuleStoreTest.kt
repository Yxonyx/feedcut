package io.github.yxonyx.feedcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuleStoreTest {
    private val bundled = File("src/main/assets/rules.json").readText()

    @Test
    fun bundledRulesParse() {
        val rules = RuleStore.parse(bundled)
        assertTrue(rules.version > 0)
        val facebook = rules.apps.single { it.packageName == "com.facebook.katana" }
        assertEquals(listOf("Marketplace"), facebook.allowTabs)
        assertTrue(rules.apps.all { it.action == "back" || it.action == "home" })
    }

    @Test
    fun bundledPackagesAreUnique() {
        val packages = RuleStore.parse(bundled).apps.map { it.packageName }
        assertEquals(packages.size, packages.toSet().size)
    }

    @Test
    fun roundTripKeepsEverything() {
        val rules = RuleStore.parse(bundled)
        assertEquals(rules, RuleStore.parse(RuleStore.toJson(rules)))
    }

    @Test
    fun missingVersionMeansZero() {
        assertEquals(0, RuleStore.parse("""{"apps": []}""").version)
    }

    @Test
    fun badInputIsRejected() {
        assertThrows(RuleError::class.java) { RuleStore.parse("not json") }
        assertThrows(RuleError::class.java) { RuleStore.parse("""{"rules": []}""") }
        assertThrows(RuleError::class.java) { RuleStore.parse("""{"apps": [{"package": "a b"}]}""") }
        assertThrows(RuleError::class.java) {
            RuleStore.parse("""{"apps": [{"package": "x.y", "action": "sideways"}]}""")
        }
    }

    @Test
    fun rulesCannotLockTheUserOut() {
        for (protected in listOf(BuildConfig.APPLICATION_ID, "com.android.settings", "com.android.systemui")) {
            assertThrows(RuleError::class.java) {
                RuleStore.parse("""{"apps": [{"package": "$protected", "enabled": true, "hints": ["*"]}]}""")
            }
        }
    }

    @Test
    fun dailyLimitIsReadAndKept() {
        val raw = """{"apps":[{"package":"a.b","hints":["x"],"dailyMinutes":20}]}"""
        val rules = RuleStore.parse(raw)
        assertEquals(20, rules.apps.single().dailyMinutes)
        assertEquals(rules, RuleStore.parse(RuleStore.toJson(rules)))
        assertEquals(0, RuleStore.parse("""{"apps":[{"package":"a.b"}]}""").apps.single().dailyMinutes)
    }

    @Test
    fun badDailyLimitIsRejected() {
        for (minutes in listOf(-1, 1441)) {
            val e = assertThrows(RuleError::class.java) {
                RuleStore.parse("""{"apps":[{"package":"a.b","dailyMinutes":$minutes}]}""")
            }
            assertEquals(R.string.err_limit, e.res)
        }
    }
}
