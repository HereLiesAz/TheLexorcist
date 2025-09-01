package com.hereliesaz.lexorcist.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.SaveState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class ScriptEditorViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scriptEditorViewModel: ScriptEditorViewModel
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveScript calls settingsManager to save script`() = runTest {
        // Given
        val script = "test script"
        coEvery { settingsManager.getScript() } returns ""
        scriptEditorViewModel = ScriptEditorViewModel(settingsManager)
        testDispatcher.scheduler.advanceUntilIdle()
        scriptEditorViewModel.onScriptTextChanged(script)
        coEvery { settingsManager.saveScript(any()) } returns Unit

        // When
        scriptEditorViewModel.saveScript()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { settingsManager.saveScript(script) }
        assertEquals(SaveState.Success, scriptEditorViewModel.saveState.value)
    }

    @Test
    fun `loadScript updates scriptText`() = runTest {
        // Given
        val script = "test script"
        coEvery { settingsManager.getScript() } returns script
        scriptEditorViewModel = ScriptEditorViewModel(settingsManager)

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(script, scriptEditorViewModel.scriptText.value)
    }
}
