package com.hereliesaz.lexorcist.service

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.hereliesaz.lexorcist.auth.CredentialHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailService @Inject constructor(
    private val credentialHolder: CredentialHolder
) {
    private val applicationName = "The Lexorcist"
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private fun getGmailService(): Gmail? {
        return credentialHolder.credential?.let {
            Gmail.Builder(httpTransport, jsonFactory, it)
                .setApplicationName(applicationName)
                .build()
        }
    }

    suspend fun searchEmails(
        from: String,
        subject: String,
        before: String,
        after: String
    ): List<Message> = withContext(Dispatchers.IO) {
        val service = getGmailService() ?: return@withContext emptyList()
        val query = mutableListOf<String>()
        if (from.isNotBlank()) query.add("from:$from")
        if (subject.isNotBlank()) query.add("subject:$subject")
        if (before.isNotBlank()) query.add("before:$before")
        if (after.isNotBlank()) query.add("after:$after")

        val messageList = service.users().messages().list("me").setQ(query.joinToString(" ")).execute()
        val fullMessages = mutableListOf<Message>()
        messageList.messages?.forEach { message ->
            val fullMessage = service.users().messages().get("me", message.id).execute()
            fullMessages.add(fullMessage)
        }
        fullMessages
    }
}