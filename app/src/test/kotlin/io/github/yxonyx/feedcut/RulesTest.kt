package io.github.yxonyx.feedcut

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

    private val blockFeed = listOf(FIRST_TAB, "Reels", "Video")

    @Test
    fun feedAndVideoTabsGoToTheAllowedTab() {
        assertEquals(3, tabRedirect(listOf("Marketplace"), bar("Kezdőlap"), blockFeed))
        assertEquals(3, tabRedirect(listOf("Marketplace"), bar("Reels"), blockFeed))
    }

    @Test
    fun tabsOutsideTheBlockListStayOpen() {
        for (tab in listOf("Ismerősök", "Marketplace", "Értesítések", "Profil")) {
            assertNull(tab, tabRedirect(listOf("Marketplace"), bar(tab), blockFeed))
        }
    }

    @Test
    fun theFirstTabIsBlockedInAnyLanguage() {
        val german = listOf(SeenTab("Startseite", true), SeenTab("Marketplace", false))
        assertEquals(1, tabRedirect(listOf("Marketplace"), german, blockFeed))
    }

    @Test
    fun aBlockListLeavesOtherTabBarsAlone() {
        val groups = listOf(SeenTab("Neked", true), SeenTab("A csoportjaid", false))
        assertNull(tabRedirect(listOf("Marketplace"), groups, blockFeed))
    }

    @Test
    fun blockedTabsArriveWithAnUpdateAndMyChangesStay() {
        val tabs = AppRule("com.facebook.katana", "Facebook", true, "back", emptyList(), listOf("Marketplace"))
        val fresh = tabs.copy(blockTabs = blockFeed)
        val mine = tabs.copy(blockTabs = listOf(FIRST_TAB))
        assertEquals(blockFeed, mergeRules(RuleSet(listOf(tabs)), RuleSet(listOf(tabs)), RuleSet(listOf(fresh))).apps[0].blockTabs)
        assertEquals(listOf(FIRST_TAB), mergeRules(RuleSet(listOf(tabs)), RuleSet(listOf(mine)), RuleSet(listOf(fresh))).apps[0].blockTabs)
    }

    @Test
    fun theWidgetListsInstalledAppsWithALimit() {
        val limited = ig.copy(dailyMinutes = 20)
        val rules = listOf(
            limited,
            yt,
            limited.copy(packageName = "off", enabled = false),
            limited.copy(packageName = "gone"),
            limited.copy(packageName = "tabs", allowTabs = listOf("Marketplace")),
        )
        assertEquals(listOf(limited), limitsToShow(rules) { it != "gone" })
    }

    @Test
    fun theWidgetHasThreeRows() {
        val many = (1..5).map { ig.copy(packageName = "app$it", dailyMinutes = 5) }
        assertEquals(listOf("app1", "app2", "app3"), limitsToShow(many) { true }.map { it.packageName })
    }

    @Test
    fun minutesLeftRoundsUpUntilTheLimitIsSpent() {
        val five = ig.copy(dailyMinutes = 5)
        assertEquals(5, minutesLeft(five, 0))
        assertEquals(5, minutesLeft(five, MS_PER_MINUTE - 1))
        assertEquals(4, minutesLeft(five, MS_PER_MINUTE))
        assertEquals(1, minutesLeft(five, limitMs(five) - 1))
        assertEquals(0, minutesLeft(five, limitMs(five)))
        assertEquals(0, minutesLeft(five, limitMs(five) + MS_PER_MINUTE))
    }

    @Test
    fun theWidgetLeadsWithTheLimitClosestToItsEnd() {
        val first = ig.copy(packageName = "first", dailyMinutes = 10)
        val second = ig.copy(packageName = "second", dailyMinutes = 20)
        val used = mapOf("first" to 0L, "second" to 19 * MS_PER_MINUTE)
        assertEquals(listOf("second", "first"), byTimeLeft(listOf(first, second)) { used.getValue(it) }.map { it.packageName })
    }

    @Test
    fun theBarShowsWhatIsLeft() {
        val ten = ig.copy(dailyMinutes = 10)
        assertEquals(BAR_STEPS, stepsLeft(ten, 0))
        assertEquals(BAR_STEPS / 2, stepsLeft(ten, 5 * MS_PER_MINUTE))
        assertEquals(0, stepsLeft(ten, limitMs(ten)))
        assertEquals(0, stepsLeft(ten, limitMs(ten) + MS_PER_MINUTE))
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
    fun ownRuleForANewDefaultAppStaysTheUsers() {
        val own = AppRule("com.example.feed", "My feed", true, "home", listOf("my_feed"))
        val newDefault = AppRule("com.example.feed", "Feed", true, "back", listOf("feed_pager"), note = "Feed")
        val merged = mergeRules(RuleSet(listOf(ig), 1), RuleSet(listOf(ig, own), 1), RuleSet(listOf(ig, newDefault), 2))
        assertEquals(own, merged.apps.single { it.packageName == "com.example.feed" })
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

    @Test
    fun healthFlagsAppUpdatesSinceLastBlock() {
        assertEquals(RuleHealth.NOT_INSTALLED, ruleHealth(null, 5L, 1L))
        assertEquals(RuleHealth.NEVER_BLOCKED, ruleHealth(2L, null, null))
        assertEquals(RuleHealth.OK, ruleHealth(2L, 5L, 2L))
        assertEquals(RuleHealth.APP_UPDATED, ruleHealth(3L, 5L, 2L))
        assertEquals(RuleHealth.OK, ruleHealth(3L, 5L, null))
    }

    @Test
    fun tappingBackInStaysInTheApp() {
        assertEquals(GateAction.BACK, escalate(GateAction.BACK, backsInARow = 0))
        assertEquals(GateAction.BACK, escalate(GateAction.BACK, backsInARow = 1))
        assertEquals(GateAction.BACK, escalate(GateAction.BACK, backsInARow = BACKS_BEFORE_HOME - 1))
    }

    @Test
    fun aScreenThatIgnoresBackGoesHome() {
        assertEquals(GateAction.HOME, escalate(GateAction.BACK, backsInARow = BACKS_BEFORE_HOME))
        assertEquals(GateAction.HOME, escalate(GateAction.HOME, backsInARow = 0))
    }

    @Test
    fun dailyLimitSurvivesUpdate() {
        val ig = rule().copy(dailyMinutes = 0)
        val mine = ig.copy(dailyMinutes = 20)
        val merged = mergeRules(RuleSet(listOf(ig), 1), RuleSet(listOf(mine), 1), RuleSet(listOf(ig), 2))
        assertEquals(20, merged.apps.single().dailyMinutes)
        assertEquals(20, mergeRules(null, RuleSet(listOf(mine), 1), RuleSet(listOf(ig), 2)).apps.single().dailyMinutes)
    }

    @Test
    fun limitWarnsOnceWhenTheLastMinuteStarts() {
        val limit = 10 * 60_000L
        assertEquals(false, limitWarns(limit, limit - 90_000, limit - 89_000))
        assertEquals(true, limitWarns(limit, limit - 60_500, limit - 59_500))
        assertEquals(false, limitWarns(limit, limit - 59_500, limit - 58_500))
    }

    @Test
    fun aOneMinuteLimitHasNoWarning() {
        assertEquals(false, limitWarns(60_000, 0, 1_000))
    }

    @Test
    fun limitIsInMilliseconds() {
        assertEquals(1_200_000L, limitMs(rule().copy(dailyMinutes = 20)))
    }
}
