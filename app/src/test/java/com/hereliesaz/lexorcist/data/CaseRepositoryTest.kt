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
    private lateinit var caseDao: CaseDao
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        caseDao = mockk()
        context = mockk(relaxed = true)
        caseRepository = CaseRepositoryImpl(context, caseDao, null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCases returns cases from dao`() = runTest {
        // Given
        val cases = listOf(
            Case(id = 1, name = "Case 1", spreadsheetId = "sheet1"),
            Case(id = 2, name = "Case 2", spreadsheetId = "sheet2")
        )
        coEvery { caseDao.getAllCases() } returns flowOf(cases)

        // When
        val result = caseRepository.getCases().first()
        testDispatcher.scheduler.advanceUntilIdle()


        // Then
        assertEquals(cases, result)
    }
}
