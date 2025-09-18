package com.hereliesaz.lexorcist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidenceProcessingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAddTextEvidence() {
        val caseName = "Test Case"
        val evidenceText = "This is a test evidence."

        // It's possible the sign-in screen is showing. If so, click the sign-in button.
        // This is a bit of a hack. A better approach would be to use a test-specific
        // Hilt module to provide a signed-in state.
        try {
            composeTestRule.onNodeWithText("SIGN IN WITH GOOGLE").performClick()
        } catch (e: AssertionError) {
            // Already signed in.
        }


        // 1. Click the "Create New Case" button.
        composeTestRule.onNodeWithText("CREATE NEW CASE").performClick()

        // 2. Enter a case name in the dialog.
        composeTestRule.onNodeWithText("Case Name").performTextInput(caseName)

        // 3. Click "Create".
        composeTestRule.onNodeWithText("CREATE").performClick()

        // 4. Navigate to the "evidence" screen.
        composeTestRule.onNodeWithText("Evidence").performClick()

        // 5. Click the "Add Text Evidence" button.
        composeTestRule.onNodeWithText("ADD TEXT EVIDENCE", useUnmergedTree = true).performClick()

        // 6. Enter text into the OutlinedTextField. The label is "Evidence Text"
        composeTestRule.onNodeWithText("Evidence Text").performTextInput(evidenceText)

        // 7. Click the "Save" button.
        composeTestRule.onNodeWithText("SAVE").performClick()

        // 8. Verify that the new evidence is displayed.
        composeTestRule.onNodeWithText(evidenceText).assertIsDisplayed()
    }
}
