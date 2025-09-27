package com.hereliesaz.lexorcist.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.location.Location // ADDED
import android.provider.CallLog
import android.provider.Telephony
import com.google.android.gms.location.FusedLocationProviderClient
import com.hereliesaz.lexorcist.data.Evidence
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EvidenceImporter(
    private val contentResolver: ContentResolver,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    fun importSms(): List<Evidence> {
        val smsList = mutableListOf<Evidence>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            "date DESC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown Address"
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val messageType = when (it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "SMS (Received)"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "SMS (Sent)"
                    else -> "MMS"
                }
                val smsContent = "From/To: $address\n\n$body"
                smsList.add(
                    Evidence(
                        caseId = 0L, // Placeholder
                        spreadsheetId = "", // Placeholder
                        type = messageType,
                        content = smsContent,
                        formattedContent = smsContent, // Placeholder
                        mediaUri = null,
                        timestamp = date,
                        sourceDocument = "SMS - $address", // Placeholder
                        documentDate = date,
                        allegationId = null,
                        allegationElementName = null,
                        category = "Communication", // Placeholder
                        tags = listOf("sms", address.lowercase(Locale.getDefault())) // Placeholder
                    )
                )
            }
        }
        return smsList
    }

    fun importCallLog(): List<Evidence> {
        val callLogList = mutableListOf<Evidence>()
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "date DESC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown Number"
                val callTypeString = when (it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming Call"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing Call"
                    CallLog.Calls.MISSED_TYPE -> "Missed Call"
                    CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
                    CallLog.Calls.REJECTED_TYPE -> "Rejected Call"
                    CallLog.Calls.BLOCKED_TYPE -> "Blocked Call"
                    else -> "Call"
                }
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val callContent = "Number: $number\nType: $callTypeString\nDuration: $duration seconds"
                callLogList.add(
                    Evidence(
                        caseId = 0L, // Placeholder
                        spreadsheetId = "", // Placeholder
                        type = callTypeString,
                        content = callContent,
                        formattedContent = callContent, // Placeholder
                        mediaUri = null,
                        timestamp = date,
                        sourceDocument = "Call Log - $number", // Placeholder
                        documentDate = date,
                        allegationId = null,
                        allegationElementName = null,
                        category = "Communication", // Placeholder
                        tags = listOf("call", number.lowercase(Locale.getDefault()), callTypeString.lowercase(Locale.getDefault()).replace(" ", "_")) // Placeholder
                    )
                )
            }
        }
        return callLogList
    }

    /**
     * Fetches the last known location of the device.
     * Note: This provides a single point-in-time location, not a historical track.
     * A full location history would require a different approach, such as Google Takeout.
     */
    @SuppressLint("MissingPermission")
    suspend fun importLocationHistory(): Evidence? = suspendCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation // CORRECTED TYPO
            .addOnSuccessListener { location: Location? -> // ADDED Location type
                if (location != null) {
                    val locationContent = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}\nAccuracy: ${location.accuracy}m"
                    val evidence = Evidence(
                        caseId = 0L, // Placeholder
                        spreadsheetId = "", // Placeholder
                        type = "Location Entry",
                        content = locationContent,
                        formattedContent = locationContent, // Placeholder
                        mediaUri = null,
                        timestamp = location.time,
                        sourceDocument = "Device Location Entry (FusedLocationProvider)", // Placeholder
                        documentDate = location.time,
                        allegationId = null,
                        allegationElementName = null,
                        category = "Location", // Placeholder
                        tags = listOf("location", "device_location") // Placeholder
                    )
                    continuation.resume(evidence)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}