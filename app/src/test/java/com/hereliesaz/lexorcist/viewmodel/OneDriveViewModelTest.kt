package com.hereliesaz.lexorcist.viewmodel

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.model.OneDriveSignInState
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OneDriveViewModelTest {

    @Mock
    private lateinit var oneDriveAuthManager: OneDriveAuthManager

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var authenticationResult: IAuthenticationResult

    @Mock
    private lateinit var account: IAccount

    private lateinit var viewModel: OneDriveViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        doReturn(editor).`when`(sharedPreferences).edit()
        doReturn(editor).`when`(editor).putString(anyString(), anyString())

        viewModel = OneDriveViewModel(oneDriveAuthManager, sharedPreferences, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `connectToOneDrive success updates state and stores token`() = runTest {
        // Arrange
        doReturn(account).`when`(authenticationResult).account
        doReturn("testuser").`when`(account).username
        doReturn("token123").`when`(authenticationResult).accessToken

        doAnswer { invocation ->
            val callback = invocation.arguments[1] as AuthenticationCallback
            callback.onSuccess(authenticationResult)
            null
        }.`when`(oneDriveAuthManager).signIn(any(), any())

        // Act
        viewModel.connectToOneDrive(activity)

        // Assert
        assertTrue(viewModel.oneDriveSignInState.value is OneDriveSignInState.Success)
        verify(editor).putString(eq("onedrive_access_token"), eq("token123"))
    }

    @Test
    fun `connectToOneDrive error updates state`() = runTest {
        // Arrange
        val exception = org.mockito.Mockito.mock(MsalException::class.java)
        doAnswer { invocation ->
            val callback = invocation.arguments[1] as AuthenticationCallback
            callback.onError(exception)
            null
        }.`when`(oneDriveAuthManager).signIn(any(), any())

        // Act
        viewModel.connectToOneDrive(activity)

        // Assert
        assertTrue(viewModel.oneDriveSignInState.value is OneDriveSignInState.Error)
    }
}
