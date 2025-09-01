package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.auth.api.identity.SignInCredential
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.UserInfo
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
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
    fun `onSignOut sets state to Idle`() = runTest {
        // When
        authViewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(authViewModel.signInState.value is SignInState.Idle)
    }

    @Test
    fun `onSignInResult with valid credentials sets state to Success`() = runTest {
        // Given
        val credential = mockk<SignInCredential>(relaxed = true)
        val expectedUserInfo = UserInfo(
            displayName = "Test User",
            email = "test@example.com",
            photoUrl = null
        )
        io.mockk.every { credential.displayName } returns "Test User"
        io.mockk.every { credential.id } returns "test@example.com"
        io.mockk.every { credential.profilePictureUri } returns null

        // When
        authViewModel.onSignInResult(credential)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = authViewModel.signInState.value
        assertTrue(state is SignInState.Success)
        assertEquals(expectedUserInfo, (state as SignInState.Success).userInfo)
    }

    @Test
    fun `onSignInError sets state to Error`() = runTest {
        // Given
        val error = Exception("Test error")

        // When
        authViewModel.onSignInError(error)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = authViewModel.signInState.value
        assertTrue(state is SignInState.Error)
        assertEquals("Sign-in attempt failed. Please try again.", (state as SignInState.Error).message)
        assertEquals(error, state.exception)
    }
}
