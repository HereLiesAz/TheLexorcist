package com.hereliesaz.lexorcist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaseCreationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCase() {
        composeTestRule.onNodeWithText("Cases").performClick()
        composeTestRule.onNodeWithText("Create New Case").performClick()
        composeTestRule.onNodeWithText("Case Name").performClick()
        // composeTestRule.onNodeWithText("Case Name").performTextInput("Test Case")
        // composeTestRule.onNodeWithText("Create").performClick()
        // composeTestRule.onNodeWithText("Test Case").assertIsDisplayed()
    }
}
