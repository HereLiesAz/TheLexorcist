/*
package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class CaseRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var caseRepository: CaseRepositoryImpl
    private lateinit var googleApiService: GoogleApiService
    private lateinit var spreadsheetParser: SpreadsheetParser
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        googleApiService = mockk(relaxed = true)
        spreadsheetParser = mockk(relaxed = true)
        context = mockk(relaxed = true)
        caseRepository = CaseRepositoryImpl(context, googleApiService, spreadsheetParser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllCases returns cases from googleApiService`() = runTest {
        // Given
        val cases = listOf(
            Case(id = 1, name = "Case 1", spreadsheetId = "sheet1", folderId = "folder1", plaintiffs = "p1", defendants = "d1", court = "c1", isArchived = false, lastModifiedTime = 1L, scriptId = "s1", generatedPdfId = "g1", sourceHtmlSnapshotId = "sh1", originalMasterHtmlTemplateId = "om1"),
            Case(id = 2, name = "Case 2", spreadsheetId = "sheet2", folderId = "folder2", plaintiffs = "p2", defendants = "d2", court = "c2", isArchived = false, lastModifiedTime = 2L, scriptId = "s2", generatedPdfId = "g2", sourceHtmlSnapshotId = "sh2", originalMasterHtmlTemplateId = "om2")
        )
        coEvery { googleApiService.getOrCreateAppRootFolder() } returns "root_folder"
        coEvery { googleApiService.getOrCreateCaseRegistrySpreadsheetId("root_folder") } returns "registry_id"
        coEvery { googleApiService.getAllCasesFromRegistry("registry_id") } returns cases

        // When
        caseRepository.refreshCases()
        val result = caseRepository.getAllCases().first()
        testDispatcher.scheduler.advanceUntilIdle()


        // Then
        assertEquals(cases, result)
    }
}
*/
