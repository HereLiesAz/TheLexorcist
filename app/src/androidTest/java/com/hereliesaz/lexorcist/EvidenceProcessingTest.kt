package com.hereliesaz.lexorcist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hereliesaz.lexorcist.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvidenceProcessingTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCreateCase() {
        // For now, I will just test that the app launches without crashing.
        // I will add more to this test later.
    }
}
