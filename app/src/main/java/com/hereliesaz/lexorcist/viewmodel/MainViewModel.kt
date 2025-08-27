package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
                    // Handle unsupported file types
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
                Evidence(text)
            }
        } catch (e: Exception) {
            // Handle exceptions
            null
        }
    }

    private suspend fun parsePdfFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                val text = buildString {
                    for (i in 1..pdfDocument.numberOfPages) {
                        val page = pdfDocument.getPage(i)
                        append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page))
                    }
                }
                pdfDocument.close()
                Evidence(text)
            }
        } catch (e: Exception) {
            // Handle exceptions
            null
        }
    }

    private suspend fun parseImageFile(uri: Uri, context: Context): Evidence? {
        return try {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(Evidence(visionText.text))
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(null)
                    }
            }
        } catch (e: Exception) {
            // Handle exceptions
            null
        }
    }

    private suspend fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
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
                Evidence(text)
            }
        } catch (e: Exception) {
            // Handle exceptions
            null
        }
    }

    private suspend fun parseDocFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    val doc = org.apache.poi.hwpf.HWPFDocument(inputStream)
                    val extractor = org.apache.poi.hwpf.extractor.WordExtractor(doc)
                    extractor.text
                } else {
                    val docx = org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)
                    val extractor = org.apache.poi.xwpf.extractor.XWPFWordExtractor(docx)
                    extractor.text
                }
                Evidence(text)
            }
        } catch (e: Exception) {
            // Handle exceptions
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
                        smsList.add(Evidence(body))
                    } while (it.moveToNext())
                }
            }
            _evidenceList.value = _evidenceList.value + smsList
        }
    }

    private var googleApiService: com.hereliesaz.lexorcist.service.GoogleApiService? = null

    fun onSignInResult(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, context: Context) {
        viewModelScope.launch {
            val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
                .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets"))
            credential.selectedAccount = account.account
            googleApiService = com.hereliesaz.lexorcist.service.GoogleApiService(credential)
        }
    }

    fun exportToSheet() {
        viewModelScope.launch {
            googleApiService?.let {
                val spreadsheetId = it.createSpreadsheet("Lexorcist Export")
                spreadsheetId?.let { id ->
                    val data = _evidenceList.value.map { evidence ->
                        listOf(evidence.text)
                    }
                    it.writeData(id, data)
                }
            }
        }
    }

    private val scriptRunner = com.hereliesaz.lexorcist.service.ScriptRunner()
    private var script: String = ""

    fun setScript(script: String) {
        this.script = script
    }

    fun processEvidenceForReview(dataReviewViewModel: DataReviewViewModel) {
        viewModelScope.launch {
            val taggedList = _evidenceList.value.map { evidence ->
                val parser = scriptRunner.runScript(script, evidence)
                com.hereliesaz.lexorcist.data.TaggedEvidence(evidence, parser.getTags())
            }
            dataReviewViewModel.setTaggedEvidence(taggedList)
        }
    }
}
