package com.hereliesaz.lexorcist.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import com.google.api.services.script.model.Content
import com.google.api.services.script.model.File
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
    private lateinit var googleApiService: GoogleApiService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        googleApiService = mockk(relaxed = true)
        scriptEditorViewModel = ScriptEditorViewModel()
        scriptEditorViewModel.initialize(googleApiService, "test_script_id")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveScript calls googleApiService to update script`() = runTest {
        // Given
        val script = "test script"
        scriptEditorViewModel.onScriptTextChanged(script)
        coEvery { googleApiService.updateScript(any(), any()) } returns true

        // When
        scriptEditorViewModel.saveScript()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { googleApiService.updateScript("test_script_id", script) }
        assertEquals(SaveState.Success, scriptEditorViewModel.saveState.value)
    }

    @Test
    fun `loadScript updates scriptText`() = runTest {
        // Given
        val scriptContent = Content().setFiles(listOf(File().setSource("test script")))
        coEvery { googleApiService.getScript(any()) } returns scriptContent

        // When
        scriptEditorViewModel.initialize(googleApiService, "test_script_id") // loadScript is called in initialize
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("test script", scriptEditorViewModel.scriptText.value)
    }
}
