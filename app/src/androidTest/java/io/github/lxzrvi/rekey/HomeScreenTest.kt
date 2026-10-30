package io.github.lxzrvi.rekey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun awaitHome() {
        composeRule.waitUntil(6_000) {
            composeRule.onAllNodesWithTag("splash").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("home").assertIsDisplayed()
    }

    @Test
    fun splashTransitionsToSafeHeaderWithExactBrandName() {
        composeRule.onNodeWithTag("splash").assertExists()
        awaitHome()
        composeRule.onNodeWithTag("home_header").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("REKEY app logo").assertIsDisplayed()
        composeRule.onNodeWithText("v0.2.0").assertIsDisplayed()
        val headerTop = composeRule.onNodeWithTag("home_header").fetchSemanticsNode().boundsInRoot.top
        val statusTop = androidx.core.view.ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())?.top ?: 0
        assertTrue("Header must not be cropped by status bar", headerTop >= statusTop)
    }

    @Test
    fun menuTravelsLeftWithDrawerAndMorphsToClose() {
        awaitHome()
        val closed = composeRule.onNodeWithTag("menu_toggle").fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag("menu_toggle").performClick()
        composeRule.waitUntil(3_000) { composeRule.onAllNodesWithTag("appearance_drawer").fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
        val open = composeRule.onNodeWithTag("menu_toggle").fetchSemanticsNode().boundsInRoot
        assertTrue("Shared menu button must travel left", open.left < closed.left)
        composeRule.onNodeWithContentDescription("Close appearance settings").assertIsDisplayed()
        composeRule.onNodeWithTag("appearance_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_toggle").performClick()
        composeRule.waitUntil(3_000) { composeRule.onAllNodesWithTag("appearance_drawer").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun appearanceChangesPersistAcrossRecreation() {
        awaitHome()
        composeRule.onNodeWithTag("menu_toggle").performClick()
        composeRule.onNodeWithTag("theme_dark").performClick()
        composeRule.onNodeWithTag("blur_switch").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(3_000) { composeRule.onAllNodesWithTag("appearance_drawer").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("theme_dark").assertIsSelected()
        composeRule.onNodeWithTag("blur_switch").assertIsOff()
    }

    @Test
    fun addCardAppendsAndPersistsThenReverseSwipeReturns() {
        awaitHome()
        composeRule.onNodeWithTag("card_carousel").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_card").performClick()
        composeRule.waitUntil(4_000) {
            composeRule.onAllNodesWithTag("demo_card_2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Card 3 of 3").assertIsDisplayed()
        composeRule.activityRule.scenario.recreate()
        awaitHome()
        composeRule.onNodeWithText("Card 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("card_carousel").performTouchInput { swipeRight() }
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Card 2 of 3").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Card 2 of 3").assertIsDisplayed()
    }

    @Test
    fun resetAppearanceRestoresPureBlurDefaults() {
        awaitHome()
        composeRule.onNodeWithTag("menu_toggle").performClick()
        composeRule.onNodeWithTag("blur_switch").performClick()
        composeRule.onNodeWithTag("blur_switch").assertIsOff()
        composeRule.onNodeWithTag("reset_appearance").performScrollTo().performClick()
        composeRule.onNodeWithTag("blur_switch").assertIsOn()
        composeRule.onNodeWithTag("theme_system").assertIsSelected()
    }
}
