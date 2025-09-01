package com.hereliesaz.lexorcist.viewmodel

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import io.mockk.coEvery
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
import org.junit.Test

@ExperimentalCoroutinesApi
class AddonsBrowserViewModelTest {

    private lateinit var viewModel: AddonsBrowserViewModel
    private lateinit var googleApiService: GoogleApiService
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        googleApiService = mockk(relaxed = true)
        viewModel = AddonsBrowserViewModel(googleApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAddons updates scripts and templates state`() = runTest {
        // Given
        val fakeScripts = listOf(Script("1", "Script 1", "", "", "", 0.0, 0))
        val fakeTemplates = listOf(Template("1", "Template 1", "", "", "", 0.0, 0))
        coEvery { googleApiService.getSharedScripts() } returns fakeScripts
        coEvery { googleApiService.getSharedTemplates() } returns fakeTemplates

        // When
        // The loadAddons function is called in the init block, so we just need to advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(fakeScripts, viewModel.scripts.value)
        assertEquals(fakeTemplates, viewModel.templates.value)
    }
}
