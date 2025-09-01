package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        val context: Context = mockk(relaxed = true)
        val sharedPrefs: android.content.SharedPreferences = mockk(relaxed = true)
        io.mockk.every { application.getSharedPreferences(any(), any()) } returns sharedPrefs
        io.mockk.every { application.applicationContext } returns context
        caseRepository = mockk(relaxed = true)
        caseViewModel = CaseViewModel(application, caseRepository)
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
        val case = Case(id = 1, name = "Test Case", spreadsheetId = "123", folderId = "f1", plaintiffs = "p1", defendants = "d1", court = "c1", isArchived = false, lastModifiedTime = 1L, scriptId = "s1", generatedPdfId = "g1", sourceHtmlSnapshotId = "sh1", originalMasterHtmlTemplateId = "om1")

        // When
        caseViewModel.archiveCaseWithRepository(case)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { caseRepository.archiveCase(case) }
    }

    @Test
    fun `deleteCase calls repository to delete case`() = runTest {
        // Given
        val case = Case(id = 1, name = "Test Case", spreadsheetId = "123", folderId = "f1", plaintiffs = "p1", defendants = "d1", court = "c1", isArchived = false, lastModifiedTime = 1L, scriptId = "s1", generatedPdfId = "g1", sourceHtmlSnapshotId = "sh1", originalMasterHtmlTemplateId = "om1")

        // When
        caseViewModel.deleteCaseWithRepository(case)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { caseRepository.deleteCase(case) }
    }
}
