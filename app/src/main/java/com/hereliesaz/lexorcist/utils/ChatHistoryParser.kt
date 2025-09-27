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
                        evidenceList.addAll(parseFacebookInstagramJson(content))
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Not a valid ZIP file, attempting to read as plain text for WhatsApp.", e)
            // Reset stream and try parsing as a plain text file (for WhatsApp)
            context.contentResolver.openInputStream(uri)?.use { textInputStream ->
                val reader = BufferedReader(InputStreamReader(textInputStream))
                val content = reader.readText()
                evidenceList.addAll(parseWhatsAppTxt(content))
            }
        }

        return evidenceList
    }

    private fun parseFacebookInstagramJson(jsonString: String): List<Evidence> {
        val evidenceList = mutableListOf<Evidence>()
        try {
            val json = JSONObject(jsonString)
            // This is a simplified parser. A real implementation would need to
            // differentiate between Facebook and Instagram formats more robustly.
            if (json.has("messages")) {
                val messages = json.getJSONArray("messages")
                for (i in 0 until messages.length()) {
                    val message = messages.getJSONObject(i)
                    val sender = message.optString("sender_name", "Unknown Sender")
                    val timestamp = message.optLong("timestamp_ms", System.currentTimeMillis())
                    val text = message.optString("content", null)

                    if (text != null) {
                        evidenceList.add(
                            Evidence(
                                content = "From: $sender\n\n$text",
                                type = "Chat (JSON)",
                                timestamp = timestamp
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON chat history", e)
        }
        return evidenceList
    }

    private fun parseWhatsAppTxt(content: String): List<Evidence> {
        val evidenceList = mutableListOf<Evidence>()
        // Regex to capture "[date, time] sender: message" and handle multi-line messages
        val pattern = Pattern.compile("""^\[(\d{1,2}/\d{1,2}/\d{2,4}, \d{1,2}:\d{2}:\d{2}\s?[AP]M)] (.*?): (.*)""", Pattern.MULTILINE)
        val matcher = pattern.matcher(content)

        val messages = mutableListOf<Triple<String, String, String>>()
        while (matcher.find()) {
            messages.add(Triple(matcher.group(1)!!, matcher.group(2)!!, matcher.group(3)!!))
        }

        for (i in messages.indices) {
            val (dateTimeStr, sender, firstLine) = messages[i]
            val start = matcher.end()
            val end = if (i + 1 < messages.size) matcher.start(i + 2) else content.length
            val fullMessage = firstLine + content.substring(start, end).trim()

            val timestamp = parseWhatsAppTimestamp(dateTimeStr)

            evidenceList.add(
                Evidence(
                    content = "From: $sender\n\n$fullMessage",
                    type = "Chat (WhatsApp)",
                    timestamp = timestamp
                )
            )
        }
        return evidenceList
    }

    private fun parseWhatsAppTimestamp(dateTimeStr: String): Long {
        // Handles formats like "M/d/yy, h:mm a" or "MM/dd/yyyy, HH:mm:ss"
        val format1 = SimpleDateFormat("M/d/yy, h:mm a", Locale.US)
        val format2 = SimpleDateFormat("MM/dd/yyyy, HH:mm:ss", Locale.US)
        return try {
            format1.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                format2.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}