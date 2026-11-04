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
}
