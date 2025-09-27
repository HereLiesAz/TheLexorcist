package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.data.Evidence
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class ChatHistoryParser(private val context: Context) {

    fun parse(uri: Uri): List<Evidence> {
        val inputStream = context.contentResolver.openInputStream(uri)
        val evidenceList = mutableListOf<Evidence>()

        try {
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".json")) {
                        val reader = BufferedReader(InputStreamReader(zipInputStream))
                        val content = reader.readText()
                        // This is a simplified parser. A real implementation would need to
                        // differentiate between Facebook, Instagram, and other formats.
                        val json = JSONObject(content)
                        val messages = json.getJSONArray("messages")
                        for (i in 0 until messages.length()) {
                            val message = messages.getJSONObject(i)
                            val sender = message.getString("sender_name")
                            val timestamp = message.getLong("timestamp_ms")
                            val text = message.optString("content", "")

                            evidenceList.add(
                                Evidence(
                                    content = "From: $sender\n\n$text",
                                    type = "Chat",
                                    timestamp = timestamp
                                )
                            )
                        }
                    } else if (entry.name.endsWith(".txt")) {
                        // WhatsApp exports are typically .txt files.
                        val reader = BufferedReader(InputStreamReader(zipInputStream))
                        reader.forEachLine { line ->
                            // A more sophisticated regex would be needed to parse WhatsApp's format.
                            evidenceList.add(
                                Evidence(
                                    content = line,
                                    type = "Chat",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            // Not a zip file, try to read as a plain text file (for WhatsApp single chat export)
            context.contentResolver.openInputStream(uri)?.use { textInputStream ->
                val reader = BufferedReader(InputStreamReader(textInputStream))
                reader.forEachLine { line ->
                    evidenceList.add(
                        Evidence(
                            content = line,
                            type = "Chat",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        return evidenceList
    }
}