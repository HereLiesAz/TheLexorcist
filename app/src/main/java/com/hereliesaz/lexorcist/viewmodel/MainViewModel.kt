package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import kotlin.coroutines.resume

class MainViewModel : ViewModel() {

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList

    fun addEvidence(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            val evidence = when (mimeType) {
                "text/plain" -> parseTextFile(uri, context)
                "application/pdf" -> parsePdfFile(uri, context)
                "image/jpeg", "image/png" -> parseImageFile(uri, context)
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                else -> {
                    Log.w("MainViewModel", "Unsupported file type: $mimeType")
                    null
                }
            }
            evidence?.let {
                _evidenceList.value = _evidenceList.value + it
            }
        }
    }

    private suspend fun parseTextFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(id = 0, caseId = 0, allegationId = 0, content = text, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList())
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to parse text file", e)
            null
        }
    }

    private suspend fun parsePdfFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val text = buildString {
                    for (i in 1..pdfDocument.numberOfPages) {
                        val page = pdfDocument.getPage(i)
                        append(PdfTextExtractor.getTextFromPage(page))
                    }
                }
                pdfDocument.close()
                Evidence(id = 0, caseId = 0, allegationId = 0, content = text, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList())
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to parse PDF file", e)
            null
        }
    }

    private suspend fun parseImageFile(uri: Uri, context: Context): Evidence? {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(Evidence(id = 0, caseId = 0, allegationId = 0, content = visionText.text, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList()))
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainViewModel", "Failed to parse image file", e)
                        continuation.resume(null)
                    }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to parse image file", e)
            null
        }
    }

    private suspend fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val text = buildString {
                    for (i in 0 until workbook.numberOfSheets) {
                        val sheet = workbook.getSheetAt(i)
                        for (row in sheet) {
                            for (cell in row) {
                                append(cell.toString())
                                append("\t")
                            }
                            appendLine()
                        }
                    }
                }
                workbook.close()
                Evidence(id = 0, caseId = 0, allegationId = 0, content = text, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList())
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to parse spreadsheet file", e)
            null
        }
    }

    private suspend fun parseDocFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    val doc = HWPFDocument(inputStream)
                    val extractor = WordExtractor(doc)
                    extractor.text
                } else {
                    val docx = XWPFDocument(inputStream)
                    val extractor = XWPFWordExtractor(docx)
                    extractor.text
                }
                Evidence(id = 0, caseId = 0, allegationId = 0, content = text, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList())
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to parse document file", e)
            null
        }
    }

    fun importSmsMessages(context: Context) {
        viewModelScope.launch {
            val smsList = mutableListOf<Evidence>()
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val bodyIndex = it.getColumnIndex(android.provider.Telephony.Sms.BODY)
                    do {
                        val body = it.getString(bodyIndex)
                        smsList.add(Evidence(id = 0, caseId = 0, allegationId = 0, content = body, timestamp = System.currentTimeMillis(), sourceDocument = "", documentDate = System.currentTimeMillis(), tags = emptyList()))
                    } while (it.moveToNext())
                }
            }
            _evidenceList.value = _evidenceList.value + smsList
        }
    }

    private var googleApiService: GoogleApiService? = null

    fun onSignInResult(account: GoogleSignInAccount, context: Context) {
        viewModelScope.launch {
            val credential = GoogleAccountCredential
                .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets"))
            credential.selectedAccount = account.account
            googleApiService = GoogleApiService(credential, "Lexorcist")
        }
    }

    fun exportToSheet() {
        viewModelScope.launch {
            googleApiService?.let {
                val spreadsheetId = it.createSpreadsheet("Lexorcist Export", null)
                spreadsheetId?.let { id ->
                    val data = _evidenceList.value.map { evidence ->
                        listOf(evidence.content)
                    }
                    it.appendData(id, "Sheet1", data)
                }
            }
        }
    }

    private val scriptRunner = ScriptRunner()
    private var script: String = ""

    fun setScript(script: String) {
        this.script = script
    }

    fun processEvidenceForReview() {
        viewModelScope.launch {
//            val taggedList = _evidenceList.value.map { evidence ->
//                val entries = scriptRunner.runScript(script, evidence)
//                TaggedEvidence(evidence, entries)
//            }
//            EvidenceRepository.setTaggedEvidence(taggedList)
        }
    }
}
