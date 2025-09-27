package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import jakarta.mail.*
import jakarta.mail.search.AndTerm
import jakarta.mail.search.FromStringTerm
import jakarta.mail.search.SearchTerm
import jakarta.mail.search.SubjectTerm
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImapService @Inject constructor() {

    fun fetchEmails(
        host: String,
        user: String,
        pass: String,
        from: String,
        subject: String
        // Consider adding caseId and spreadsheetId as parameters here
    ): List<Evidence> {
        val properties = Properties()
        properties["mail.store.protocol"] = "imaps"
        val session = Session.getInstance(properties)
        val store = session.getStore("imaps")
        store.connect(host, user, pass)

        val inbox = store.getFolder("INBOX")
        inbox.open(Folder.READ_ONLY)

        val searchTerms = mutableListOf<SearchTerm>()
        if (from.isNotBlank()) {
            searchTerms.add(FromStringTerm(from))
        }
        if (subject.isNotBlank()) {
            searchTerms.add(SubjectTerm(subject))
        }
        val combinedTerm = AndTerm(searchTerms.toTypedArray())

        val messages = if (searchTerms.isNotEmpty()) {
            inbox.search(combinedTerm)
        } else {
            inbox.messages
        }

        val evidenceList = mutableListOf<Evidence>()
        for (message in messages) {
            val emailContent = "From: ${message.from.joinToString()}\nSubject: ${message.subject}\n\n${getTextFromMessage(message)}"
            evidenceList.add(
                Evidence(
                    caseId = 0L, // Placeholder - consider passing as a parameter
                    spreadsheetId = "", // Placeholder - consider passing as a parameter
                    type = "Email",
                    content = emailContent,
                    formattedContent = emailContent, // Placeholder
                    mediaUri = null,
                    timestamp = message.receivedDate.time,
                    sourceDocument = "IMAP Email - Subject: ${message.subject}", // Placeholder
                    documentDate = message.receivedDate.time, // Using receivedDate, adjust if specific logic needed
                    allegationId = null,
                    allegationElementName = null,
                    category = "Email", // Placeholder
                    tags = listOf("email", "imap") // Placeholder
                )
            )
        }

        inbox.close(false)
        store.close()

        return evidenceList
    }

    private fun getTextFromMessage(message: Message): String {
        return when {
            message.isMimeType("text/plain") -> message.content.toString()
            message.isMimeType("multipart/*") -> {
                val multipart = message.content as Multipart
                var result = ""
                for (i in 0 until multipart.count) {
                    val part = multipart.getBodyPart(i)
                    if (part.isMimeType("text/plain")) {
                        result += "\n" + part.content
                    }
                }
                result
            }
            else -> ""
        }
    }
}