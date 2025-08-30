package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class CaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var caseViewModel: CaseViewModel
    private lateinit var caseRepository: CaseRepository
    private lateinit var application: Application
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        caseRepository = mockk()
        authViewModel = mockk(relaxed = true)
        caseViewModel = CaseViewModel(application, caseRepository, authViewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCases returns cases from repository`() = runTest {
        // Given
        val cases = listOf(
            Case(id = 1, name = "Case 1", spreadsheetId = "sheet1"),
            Case(id = 2, name = "Case 2", spreadsheetId = "sheet2")
        )
        coEvery { caseRepository.getCases() } returns flowOf(cases)
        coEvery { caseRepository.refreshCases() } returns Unit

        // When
        caseViewModel.loadCases()
        testDispatcher.scheduler.advanceUntilIdle()


        // Then
        assertEquals(cases, caseViewModel.cases.value)
    }
}
