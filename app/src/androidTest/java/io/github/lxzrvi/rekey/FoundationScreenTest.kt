package io.github.lxzrvi.rekey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsWhiteWordmarkAndPendingBrandFont() {
        composeRule.onNodeWithContentDescription("REKEY").assertIsDisplayed()
        composeRule.onNodeWithText("Brand typeface pending").assertIsDisplayed()
    }

    @Test
    fun allPoppinsWeightsArePresentInComposition() {
        listOf("Thin", "Regular", "Medium", "Bold").forEach { weight ->
            composeRule.onNodeWithText(weight).assertExists()
        }
    }

    @Test
    fun motionCanBeRevealedAndReset() {
        composeRule.onNodeWithTag("motion_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("motion_result").assertExists()
        composeRule.onNodeWithTag("motion_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("motion_result").assertDoesNotExist()
    }

    @Test
    fun motionPreviewStateSurvivesActivityRecreation() {
        composeRule.onNodeWithTag("motion_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("motion_result").assertExists()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Reset the preview").assertExists()
        composeRule.onNodeWithTag("motion_result").assertExists()
    }
}
