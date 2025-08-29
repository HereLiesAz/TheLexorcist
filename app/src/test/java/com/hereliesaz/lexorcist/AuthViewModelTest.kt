package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import io.mockk.any
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authViewModel: AuthViewModel
    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var caseRepository: CaseRepositoryImpl
    private lateinit var application: Application
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        caseRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        authViewModel = AuthViewModel(application, evidenceRepository, caseRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSignInResult with valid credentials sets isSignedIn to true`() = runTest {
        // Given
        val idToken = "test_id_token"
        val email = "test@example.com"
        val applicationName = "TestApp"

        // When
        authViewModel.onSignInResult(idToken, email, context, applicationName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(authViewModel.isSignedIn.value)
        verify { evidenceRepository.setGoogleApiService(any<GoogleApiService>()) }
        verify { caseRepository.setGoogleApiService(any<GoogleApiService>()) }
    }

    @Test
    fun `onSignInResult with invalid credentials sets isSignedIn to false`() = runTest {
        // Given
        val idToken = null
        val email = null
        val applicationName = "TestApp"

        // When
        authViewModel.onSignInResult(idToken, email, context, applicationName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(authViewModel.isSignedIn.value)
        verify { evidenceRepository.setGoogleApiService(null) }
        verify { caseRepository.setGoogleApiService(null) }
    }

    @Test
    fun `onSignOut sets isSignedIn to false`() = runTest {
        // When
        authViewModel.onSignOut()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(authViewModel.isSignedIn.value)
        verify { evidenceRepository.setGoogleApiService(null) }
        verify { caseRepository.setGoogleApiService(null) }
    }
}
