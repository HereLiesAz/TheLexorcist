package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
        caseRepository = mockk(relaxed = true)
        authViewModel = mockk(relaxed = true)
        caseViewModel = CaseViewModel(application, caseRepository, authViewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createCase calls repository to create case`() = runTest {
        // Given
        val caseName = "Test Case"
        val exhibitSheetName = "Exhibits"
        val caseNumber = "12345"
        val caseSection = "A"
        val judge = "Judge Dredd"

        // When
        caseViewModel.createCase(caseName, exhibitSheetName, caseNumber, caseSection, judge)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { caseRepository.createCase(caseName, exhibitSheetName, caseNumber, caseSection, judge, any(), any(), any()) }
    }

    @Test
    fun `archiveCase calls repository to archive case`() = runTest {
        // Given
        val case = Case(id = 1, name = "Test Case", spreadsheetId = "123")

        // When
        caseViewModel.archiveCase(case)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { caseRepository.archiveCase(case) }
    }

    @Test
    fun `deleteCase calls repository to delete case`() = runTest {
        // Given
        val case = Case(id = 1, name = "Test Case", spreadsheetId = "123")

        // When
        caseViewModel.deleteCase(case)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { caseRepository.deleteCase(case) }
    }
}
