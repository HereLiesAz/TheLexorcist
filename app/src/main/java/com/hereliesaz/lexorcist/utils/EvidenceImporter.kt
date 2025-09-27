package com.hereliesaz.lexorcist.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.CallLog
import android.provider.Telephony
import com.google.android.gms.location.FusedLocationProviderClient
import com.hereliesaz.lexorcist.data.Evidence
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EvidenceImporter(
    private val contentResolver: ContentResolver,
    private val fusedLocationClient: FusedLocationProviderClient
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
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = when (it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "SMS"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "SMS"
                    else -> "MMS"
                }
                smsList.add(
                    Evidence(
                        content = "From: $address\n\n$body",
                        type = type,
                        timestamp = date
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
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val type = when (it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming Call"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing Call"
                    CallLog.Calls.MISSED_TYPE -> "Missed Call"
                    else -> "Call"
                }
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                callLogList.add(
                    Evidence(
                        content = "Number: $number\nDuration: $duration seconds",
                        type = type,
                        timestamp = date
                    )
                )
            }
        }
        return callLogList
    }

    @SuppressLint("MissingPermission")
    suspend fun importLocationHistory(): Evidence? = suspendCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val evidence = Evidence(
                        content = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}",
                        type = "Location",
                        timestamp = location.time
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