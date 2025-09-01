package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.auth.api.identity.SignInCredential
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.UserInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authViewModel: AuthViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        authViewModel = AuthViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSignInResult with valid credentials sets state to Success`() = runTest {
        // Given
        val credential = mockk<SignInCredential>()
        every { credential.displayName } returns "Test User"
        every { credential.id } returns "test@example.com"
        every { credential.profilePictureUri } returns null

        // When
        authViewModel.onSignInResult(credential)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = authViewModel.signInState.first()

        // Then
        assertTrue(state is SignInState.Success)
        assertEquals("Test User", (state as SignInState.Success).userInfo.displayName)
    }

    @Test
    fun `onSignInError sets state to Error`() = runTest {
        // Given
        val exception = Exception("Test error")

        // When
        authViewModel.onSignInError(exception)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = authViewModel.signInState.first()

        // Then
        assertTrue(state is SignInState.Error)
        assertEquals("Sign-in attempt failed. Please try again.", (state as SignInState.Error).message)
    }

    @Test
    fun `signOut sets state to Idle`() = runTest {
        // When
        authViewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = authViewModel.signInState.first()

        // Then
        assertTrue(state is SignInState.Idle)
    }
}
