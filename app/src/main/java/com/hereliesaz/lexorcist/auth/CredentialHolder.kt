package com.hereliesaz.lexorcist.auth

import android.accounts.Account
import android.app.Application
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes
import com.hereliesaz.lexorcist.model.UserInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialHolder @Inject constructor(private val application: Application) {

    /**
     * The credential for the app's own Drive files and the shared Extras
     * spreadsheet. Does not carry the Gmail scope -- see [gmailCredential].
     */
    var credential: GoogleAccountCredential? = null
        set(value) {
            field = value
            gmailCredential = null
        }

    var userInfo: UserInfo? = null

    private var gmailCredential: GoogleAccountCredential? = null

    /**
     * A separate credential holding only `gmail.readonly`.
     *
     * Read access to the user's entire mailbox used to be requested at sign-in,
     * on the same consent screen as everything else, whether or not they ever
     * imported an email. Asking for a mailbox up front to support an optional
     * import is not a reasonable trade for someone whose Google account holds
     * other clients' correspondence.
     *
     * Keeping the scope on its own credential defers the request to first use:
     * the Gmail token request fails with `UserRecoverableAuthIOException`,
     * which the app already routes to a consent prompt for other recoverable
     * auth failures. A user who never imports mail is never asked.
     */
    fun gmailCredential(): GoogleAccountCredential? {
        val base = credential ?: return null
        val account: Account = base.selectedAccount ?: return null
        return gmailCredential ?: GoogleAccountCredential
            .usingOAuth2(application, listOf(GmailScopes.GMAIL_READONLY))
            .setSelectedAccount(account)
            .also { gmailCredential = it }
    }
}
