package com.hereliesaz.lexorcist.data

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
class EvidenceRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var evidenceDao: EvidenceDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        evidenceDao = mockk()
        evidenceRepository = EvidenceRepositoryImpl(evidenceDao)
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
            Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = System.currentTimeMillis(), sourceDocument = "doc1", documentDate = System.currentTimeMillis()),
            Evidence(id = 2, caseId = 1, content = "Evidence 2", timestamp = System.currentTimeMillis(), sourceDocument = "doc2", documentDate = System.currentTimeMillis())
        )
        coEvery { evidenceDao.getEvidenceForCase(caseId) } returns flowOf(evidenceList)

        // When
        val result = evidenceRepository.getEvidenceForCase(caseId).first()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(evidenceList, result)
    }
}
