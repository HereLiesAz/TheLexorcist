package com.hereliesaz.lexorcist

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"
    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _cases = MutableStateFlow<List<File>>(emptyList())
    val cases: StateFlow<List<File>> = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<File?>(null)
    val selectedCase: StateFlow<File?> = _selectedCase.asStateFlow()

    private val _plaintiffs = MutableStateFlow("")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow("")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow("")
    val court: StateFlow<String> = _court.asStateFlow()

    init {
        loadCaseInfo()
        loadCases()
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        saveCaseInfo()
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        saveCaseInfo()
    }

    fun onCourtChanged(name: String) {
        _court.value = name
        saveCaseInfo()
    }

    private fun saveCaseInfo() {
        with(sharedPref.edit()) {
            putString("plaintiffs", _plaintiffs.value)
            putString("defendants", _defendants.value)
            putString("court", _court.value)
            apply()
        }
    }

    private fun loadCaseInfo() {
        _plaintiffs.value = sharedPref.getString("plaintiffs", "") ?: ""
        _defendants.value = sharedPref.getString("defendants", "") ?: ""
        _court.value = sharedPref.getString("court", "") ?: ""
    }

    fun onSignInSuccess(apiService: GoogleApiService) {
        _googleApiService.value = apiService
        _isSignedIn.value = true
        loadCases()
    }

    fun onSignInFailed() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    fun onSignOut() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    private fun loadCases() {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch
            val caseList = apiService.listCases()
            _cases.value = caseList
        }
    }

    fun createCase(caseName: String) {
        viewModelScope.launch {
            val apiService = googleApiService.value ?: return@launch
            val caseInfo = mapOf(
                "Plaintiffs" to _plaintiffs.value,
                "Defendants" to _defendants.value,
                "Court" to _court.value
            )
            apiService.createCase(caseName, caseInfo)
            loadCases()
        }
    }

    fun selectCase(case: File) {
        _selectedCase.value = case
    }

    private val _extractedText = MutableStateFlow("")
    val extractedText = _extractedText.asStateFlow()

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap = _imageBitmap.asStateFlow()

    fun onImageSelected(bitmap: Bitmap, context: Context) {
        _imageBitmap.value = bitmap
        viewModelScope.launch {
            processImage(bitmap, context)
        }
    }

    private suspend fun processImage(bitmap: Bitmap, context: Context) {
        Log.d(TAG, "processImage called")
        selectedCase.value?.let { case ->
            val apiService = googleApiService.value ?: return@let
            val caseFolderId = case.id
            val rawEvidenceFolderId = apiService.getOrCreateFolder(RAW_EVIDENCE_FOLDER_NAME, caseFolderId) ?: return@let
            val timestamp = System.currentTimeMillis()
            val imageName = "evidence-$timestamp.jpg"

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            apiService.uploadFile(imageName, data, "image/jpeg", rawEvidenceFolderId)

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    Log.d(TAG, "processImage: Text recognition successful")
                    _extractedText.value = visionText.text
                    val textFileName = "text-from-$imageName.txt"
                    apiService.uploadFile(textFileName, visionText.text.toByteArray(), "text/plain", rawEvidenceFolderId)
                    val taggedData = DataParser.tagData(visionText.text)
                    Log.d(TAG, "processImage: taggedData: $taggedData")
                    storeTaggedData(taggedData)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "processImage: Text recognition failed", e)
                    _extractedText.value = "Failed to extract text: ${e.message}"
                }
        }
    }

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData = _taggedData.asStateFlow()

    private fun storeTaggedData(taggedData: Map<String, List<String>>) {
        Log.d(TAG, "storeTaggedData called with: $taggedData")
        _taggedData.value = taggedData
        viewModelScope.launch {
            selectedCase.value?.let { case ->
                val spreadsheetId = findSpreadsheetForCase(case.id)
                if (spreadsheetId != null) {
                    taggedData.forEach { (tag, data) ->
                        if (data.isNotEmpty()) {
                            googleApiService.value?.addSheet(spreadsheetId, tag)
                            val values = data.map { listOf(it) }
                            googleApiService.value?.appendData(spreadsheetId, tag, values)
                        }
                    }
                }
            }
        }
    }

    private suspend fun findSpreadsheetForCase(caseFolderId: String): String? {
        val apiService = googleApiService.value ?: return null
        val files = apiService.getFilesInFolder(caseFolderId)
        return files.firstOrNull { it.mimeType == "application/vnd.google-apps.spreadsheet" }?.id
    }
}

private const val RAW_EVIDENCE_FOLDER_NAME = "Raw Evidence"