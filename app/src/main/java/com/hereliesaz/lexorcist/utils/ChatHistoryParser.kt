package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hereliesaz.lexorcist.data.Evidence
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

class ChatHistoryParser(private val context: Context) {

    private val TAG = "ChatHistoryParser"

    fun parse(uri: Uri): List<Evidence> {
        val evidenceList = mutableListOf<Evidence>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        // Try parsing as a ZIP file (for Facebook/Instagram)
        try {
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith("message_1.json")) { // A common file name in exports
                        val reader = BufferedReader(InputStreamReader(zipInputStream))
                        val content = reader.readText()
                        evidenceList.addAll(parseFacebookInstagramJson(content, entry.name))
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Not a valid ZIP file or error during ZIP processing for $uri, attempting to read as plain text for WhatsApp.", e)
            // Reset stream and try parsing as a plain text file (for WhatsApp)
            try {
                context.contentResolver.openInputStream(uri)?.use { textInputStream ->
                    val reader = BufferedReader(InputStreamReader(textInputStream))
                    val content = reader.readText()
                    evidenceList.addAll(parseWhatsAppTxt(content, uri.lastPathSegment ?: "WhatsApp Chat"))
                }
            } catch (txtException: Exception) {
                Log.e(TAG, "Failed to parse $uri as plain text WhatsApp chat after ZIP attempt failed.", txtException)
            }
        }

        return evidenceList
    }

    private fun parseFacebookInstagramJson(jsonString: String, sourceFileName: String): List<Evidence> {
        val evidenceList = mutableListOf<Evidence>()
        try {
            val json = JSONObject(jsonString)
            val messagesArray = json.optJSONArray("messages")
            if (messagesArray != null) {
                for (i in 0 until messagesArray.length()) {
                    val message = messagesArray.getJSONObject(i)
                    val sender = message.optString("sender_name", "Unknown Sender")
                    val timestamp = message.optLong("timestamp_ms", System.currentTimeMillis())
                    val text = message.optString("content", null as String?)

                    if (text != null) {
                        val chatContent = "From: $sender\n\n$text"
                        evidenceList.add(
                            Evidence(
                                caseId = 0L, // Placeholder
                                spreadsheetId = "", // Placeholder
                                type = "Chat (JSON)",
                                content = chatContent,
                                formattedContent = chatContent, // Placeholder
                                mediaUri = null,
                                timestamp = timestamp,
                                sourceDocument = "Chat History - $sourceFileName", // Placeholder
                                documentDate = timestamp,
                                allegationId = null,
                                allegationElementName = null,
                                category = "Chat", // Placeholder
                                tags = listOf("chat", "json", sender.lowercase(Locale.getDefault())) // Placeholder
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON chat history from $sourceFileName", e)
        }
        return evidenceList
    }

    private fun parseWhatsAppTxt(content: String, sourceFileName: String): List<Evidence> {
        val evidenceList = mutableListOf<Evidence>()
        // Regex to capture "[date, time] sender: message" and handle multi-line messages
        // Example: [12/31/23, 11:59:00 PM] John Doe: Hello there!
        // Example: [31/12/2023, 23:59:00] Jane Doe: Hi!
        val pattern = Pattern.compile("""^\[(\d{1,2}/\d{1,2}/\d{2,4}, \d{1,2}:\d{2}(?::\d{2})?\s?[APap]?[Mm]?)] (.*?): (.*)""", Pattern.MULTILINE)
        val matcher = pattern.matcher(content)

        var lastMatchEnd = 0
        val messageSegments = mutableListOf<Triple<String, String, String>>()

        while (matcher.find()) {
            // Add text between the last match and this match as part of the previous message (if any)
            if (messageSegments.isNotEmpty() && matcher.start() > lastMatchEnd) {
                val previousMessage = messageSegments.last()
                val continuedText = content.substring(lastMatchEnd, matcher.start()).trim()
                if (continuedText.isNotEmpty()) {
                    messageSegments[messageSegments.size -1] = previousMessage.copy(third = previousMessage.third + "\n" + continuedText)
                }
            }
            messageSegments.add(Triple(matcher.group(1)!!, matcher.group(2)!!, matcher.group(3)!!))
            lastMatchEnd = matcher.end()
        }
        // Add any remaining text after the last match to the last message
        if (messageSegments.isNotEmpty() && content.length > lastMatchEnd) {
             val previousMessage = messageSegments.last()
             val continuedText = content.substring(lastMatchEnd).trim()
             if (continuedText.isNotEmpty()) {
                  messageSegments[messageSegments.size -1] = previousMessage.copy(third = previousMessage.third + "\n" + continuedText)
             }
        }

        for ((dateTimeStr, sender, messageText) in messageSegments) {
            val timestamp = parseWhatsAppTimestamp(dateTimeStr.trim())
            val chatContent = "From: $sender\n\n$messageText"
            evidenceList.add(
                Evidence(
                    caseId = 0L, // Placeholder
                    spreadsheetId = "", // Placeholder
                    type = "Chat (WhatsApp)",
                    content = chatContent,
                    formattedContent = chatContent, // Placeholder
                    mediaUri = null,
                    timestamp = timestamp,
                    sourceDocument = "WhatsApp Chat - $sourceFileName", // Placeholder
                    documentDate = timestamp,
                    allegationId = null,
                    allegationElementName = null,
                    category = "Chat", // Placeholder
                    tags = listOf("chat", "whatsapp", sender.lowercase(Locale.getDefault())) // Placeholder
                )
            )
        }
        return evidenceList
    }

    private fun parseWhatsAppTimestamp(dateTimeStr: String): Long {
        // Handles formats like "M/d/yy, h:mm a" or "MM/dd/yyyy, HH:mm:ss"
        val format1 = SimpleDateFormat("M/d/yy, h:mm a", Locale.US)
        val format2 = SimpleDateFormat("MM/dd/yyyy, HH:mm:ss", Locale.US)
        val format3 = SimpleDateFormat("d/M/yy, H:mm", Locale.US) // e.g., 20/7/24, 15:00
        val format4 = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.US)

        return try {
            format1.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                format2.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                try {
                    format3.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
                } catch (e3: Exception) {
                    try {
                         format4.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
                    } catch (e4: Exception) {
                        Log.w(TAG, "Could not parse WhatsApp timestamp: $dateTimeStr", e4)
                        System.currentTimeMillis()
                    }
                }
            }
        }
    }
}