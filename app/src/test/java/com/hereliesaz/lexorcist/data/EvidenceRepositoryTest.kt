package com.hereliesaz.lexorcist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Evidence
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EvidenceRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var googleApiService: GoogleApiService
    private lateinit var evidenceDao: EvidenceDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        googleApiService = mockk()
        evidenceDao = mockk()
        evidenceRepository = EvidenceRepositoryImpl(evidenceDao, googleApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getEvidenceForCase returns evidence from dao`() = runTest {
        // Given
        val caseId = 1L
        val evidenceList = listOf(
            Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = Date(), sourceDocument = "doc1", documentDate = Date()),
            Evidence(id = 2, caseId = 1, content = "Evidence 2", timestamp = Date(), sourceDocument = "doc2", documentDate = Date())
        )
        coEvery { evidenceDao.getEvidenceForCase(caseId) } returns flowOf(evidenceList)

        // When
        val result = evidenceRepository.getEvidenceForCase(caseId).first()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(evidenceList, result)
    }
}
