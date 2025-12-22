package com.hereliesaz.lexorcist.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.R
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OneDriveAuthManagerTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var activity: Activity

    @Mock
    lateinit var msalApp: ISingleAccountPublicClientApplication

    @Mock
    lateinit var callback: AuthenticationCallback

    private lateinit var authManager: OneDriveAuthManager

    @Test
    fun testSignIn() {
        // Mock static PublicClientApplication and Log
        val mockedPublicClientApplication: MockedStatic<PublicClientApplication> = Mockito.mockStatic(PublicClientApplication::class.java)
        val mockedLog: MockedStatic<Log> = Mockito.mockStatic(Log::class.java)

        try {
            // Mock Log methods to prevent RuntimeException
            mockedLog.`when`<Int> { android.util.Log.d(any(), any()) }.thenReturn(0)
            mockedLog.`when`<Int> { android.util.Log.e(any(), any(), any()) }.thenReturn(0)
            mockedLog.`when`<Int> { android.util.Log.e(any(), any()) }.thenReturn(0)

            // Initialize the manager
            authManager = OneDriveAuthManager(context)

            // Capture the listener passed to createSingleAccountPublicClientApplication
            val listenerCaptor = ArgumentCaptor.forClass(IPublicClientApplication.ISingleAccountApplicationCreatedListener::class.java)
            mockedPublicClientApplication.verify {
                PublicClientApplication.createSingleAccountPublicClientApplication(
                    eq(context),
                    eq(R.raw.msal_config),
                    listenerCaptor.capture()
                )
            }

            // Mock Log.d to prevent "Method d in android.util.Log not mocked"
            mockedLog.`when`<Int> { Log.d(anyString(), anyString()) }.thenReturn(0)

            // Trigger onCreated to set the msalApplication inside the manager
            listenerCaptor.value.onCreated(msalApp)

            // Call signIn
            authManager.signIn(activity, callback)

            // Verify msalApp.signIn was called
            verify(msalApp).signIn(any(SignInParameters::class.java))

        } finally {
            mockedPublicClientApplication.close()
            mockedLog.close()
        }
    }
}
