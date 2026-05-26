package com.hereliesaz.lexorcist.auth

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DropboxAuthManagerTest {

    private val requestConfig = DbxRequestConfig.newBuilder("test-app").build()
    private val key = "dropbox_credential"

    private fun credential() =
        DbxCredential("access-token", 9_999_999_999_999L, "refresh-token", "app-key")

    @Test
    fun `not authenticated when no stored credential`() {
        val storage = mock<TinkSecureStorage>()
        whenever(storage.getString(key)).thenReturn(null)

        val manager = DropboxAuthManager(requestConfig, storage)

        assertFalse(manager.isAuthenticated.value)
        assertNull(manager.getClient())
    }

    @Test
    fun `saveCredential persists encrypted and authenticates`() {
        val storage = mock<TinkSecureStorage>()
        whenever(storage.getString(key)).thenReturn(null)
        val manager = DropboxAuthManager(requestConfig, storage)

        manager.saveCredential(credential())

        verify(storage).putString(eq(key), any())
        assertTrue(manager.isAuthenticated.value)
        assertNotNull(manager.getClient())
    }

    @Test
    fun `clearCredential removes stored credential and de-authenticates`() {
        val storage = mock<TinkSecureStorage>()
        whenever(storage.getString(key)).thenReturn(null)
        val manager = DropboxAuthManager(requestConfig, storage)
        manager.saveCredential(credential())

        manager.clearCredential()

        verify(storage).remove(key)
        assertFalse(manager.isAuthenticated.value)
        assertNull(manager.getClient())
    }

    @Test
    fun `saveCredential skips redundant write for an unchanged credential`() {
        val storage = mock<TinkSecureStorage>()
        whenever(storage.getString(key)).thenReturn(null)
        val manager = DropboxAuthManager(requestConfig, storage)

        manager.saveCredential(credential())
        manager.saveCredential(credential()) // same access + refresh token (e.g. repeated onResume)

        verify(storage, times(1)).putString(eq(key), any())
    }

    @Test
    fun `loads existing credential from storage on init`() {
        val storage = mock<TinkSecureStorage>()
        whenever(storage.getString(key)).thenReturn(DbxCredential.Writer.writeToString(credential()))

        val manager = DropboxAuthManager(requestConfig, storage)

        assertTrue(manager.isAuthenticated.value)
        assertNotNull(manager.getClient())
    }
}
