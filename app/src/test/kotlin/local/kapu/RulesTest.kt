package local.kapu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RulesTest {
    private fun rule(
        enabled: Boolean = true,
        action: String = "back",
        hints: List<String> = listOf("clips_viewer"),
    ) = AppRule("com.instagram.android", "Instagram", enabled, action, hints)

    @Test
    fun offRuleDoesNothing() {
        val nodes = listOf(SeenNode("com.instagram.android:id/clips_viewer", true))
        assertNull(chooseAction(rule(enabled = false), true, nodes))
    }

    @Test
    fun emptyHintsDoNothing() {
        assertNull(chooseAction(rule(hints = emptyList()), true, listOf(SeenNode("clips_viewer", true))))
    }

    @Test
    fun visibleIdMatchGoesBack() {
        val nodes = listOf(SeenNode("com.instagram.android:id/clips_viewer_root", true))
        assertEquals(GateAction.BACK, chooseAction(rule(), false, nodes))
    }

    @Test
    fun hiddenIdDoesNotMatch() {
        val nodes = listOf(SeenNode("com.instagram.android:id/clips_viewer", false))
        assertNull(chooseAction(rule(), true, nodes))
    }

    @Test
    fun starOnlyOnWindowChange() {
        val star = rule(action = "home", hints = listOf("*"))
        assertEquals(GateAction.HOME, chooseAction(star, true, emptyList()))
        assertNull(chooseAction(star, false, emptyList()))
    }

    @Test
    fun unknownActionFallsBackToBack() {
        val nodes = listOf(SeenNode("reel_watch_player", true))
        val odd = rule(action = "sideways", hints = listOf("reel_watch_player"))
        assertEquals(GateAction.BACK, chooseAction(odd, true, nodes))
    }

    @Test
    fun exactIdsGetPackagePrefix() {
        assertEquals(
            listOf("com.instagram.android:id/clips_viewer", "other.app:id/x"),
            exactIds("com.instagram.android", listOf("clips_viewer", "*", "other.app:id/x")),
        )
    }

    @Test
    fun idMatchesIgnoresStarAndNull() {
        assertEquals(false, idMatches(listOf("*"), "com.instagram.android:id/clips_viewer"))
        assertEquals(false, idMatches(listOf("clips_viewer"), null))
        assertEquals(true, idMatches(listOf("CLIPS_VIEWER"), "com.instagram.android:id/clips_viewer_root"))
    }

    private val facebookBar = listOf("Kezdőlap", "Reels", "Ismerősök", "Marketplace", "Értesítések", "Profil")

    private fun bar(selected: String) = facebookBar.map { SeenTab(it, it == selected) }

    @Test
    fun forbiddenTabRedirectsToAllowed() {
        assertEquals(3, tabRedirect(listOf("Marketplace"), bar("Kezdőlap")))
        assertEquals(3, tabRedirect(listOf("marketplace"), bar("Reels")))
    }

    @Test
    fun allowedTabStays() {
        assertNull(tabRedirect(listOf("Marketplace"), bar("Marketplace")))
    }

    @Test
    fun otherTabBarsAreLeftAlone() {
        val groups = listOf(SeenTab("Neked", true), SeenTab("A csoportjaid", false))
        assertNull(tabRedirect(listOf("Marketplace"), groups))
        assertNull(tabRedirect(listOf("Marketplace"), emptyList()))
    }

    @Test
    fun tabLabelTakesFirstPart() {
        assertEquals("Marketplace", tabLabel("Marketplace, 4. lap, összesen 6"))
        assertEquals("", tabLabel(null))
    }

    private val ig = AppRule("com.instagram.android", "Instagram", true, "back", listOf("clips_viewer"))
    private val yt = AppRule("com.google.android.youtube", "YouTube", true, "back", listOf("reel_watch_player"))

    @Test
    fun updateBringsNewHintsAndKeepsSwitch() {
        val base = RuleSet(listOf(ig), 1)
        val user = RuleSet(listOf(ig.copy(enabled = false)), 1)
        val bundled = RuleSet(listOf(ig.copy(hints = listOf("clips_viewer", "clips_tab")), yt), 2)
        val merged = mergeRules(base, user, bundled)
        assertEquals(2, merged.version)
        assertEquals(listOf("clips_viewer", "clips_tab"), merged.apps[0].hints)
        assertEquals(false, merged.apps[0].enabled)
        assertEquals(yt, merged.apps[1])
    }

    @Test
    fun userEditedHintsSurviveUpdate() {
        val base = RuleSet(listOf(ig), 1)
        val user = RuleSet(listOf(ig.copy(hints = listOf("my_marker"), action = "home")), 1)
        val bundled = RuleSet(listOf(ig.copy(hints = listOf("clips_tab"))), 2)
        val merged = mergeRules(base, user, bundled).apps.single()
        assertEquals(listOf("my_marker"), merged.hints)
        assertEquals("home", merged.action)
    }

    @Test
    fun deletedRuleStaysDeletedAndOwnRuleStays() {
        val own = AppRule("com.example.feed", "Feed", true, "home", listOf("*"))
        val base = RuleSet(listOf(ig, yt), 1)
        val user = RuleSet(listOf(ig, own), 1)
        val merged = mergeRules(base, user, RuleSet(listOf(ig, yt), 2))
        assertEquals(listOf("com.instagram.android", "com.example.feed"), merged.apps.map { it.packageName })
    }

    @Test
    fun oldInstallWithoutBaseGetsNewHintsKeepsActionAndSwitch() {
        val user = RuleSet(listOf(ig.copy(enabled = false, action = "home")), 0)
        val bundled = RuleSet(listOf(ig.copy(hints = listOf("clips_tab"))), 2)
        val merged = mergeRules(null, user, bundled).apps.single()
        assertEquals(listOf("clips_tab"), merged.hints)
        assertEquals("home", merged.action)
        assertEquals(false, merged.enabled)
    }
}
