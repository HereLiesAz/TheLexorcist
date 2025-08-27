package com.hereliesaz.lexorcist

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.db.AppDatabase
import com.hereliesaz.lexorcist.db.Case
// import com.hereliesaz.lexorcist.db.Filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val caseDao = db.caseDao()
    private val allegationDao = db.allegationDao()
    private val financialEntryDao = db.financialEntryDao()

    private val TAG = "MainViewModel"
    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _plaintiffs = MutableStateFlow("")
    val plaintiffs: StateFlow<String> = _plaintiffs

    private val _defendants = MutableStateFlow("")
    val defendants: StateFlow<String> = _defendants

    private val _court = MutableStateFlow("")
    val court: StateFlow<String> = _court

    init {
        loadCaseInfo()
        viewModelScope.launch {
            caseDao.getAllCases().collect {
                _cases.value = it
            }
        }
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
    }

    fun onSignInFailed() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    fun onSignOut() {
        _googleApiService.value = null
        _isSignedIn.value = false
    }

    suspend fun createMasterTemplate(parentId: String): String? {
        return _googleApiService.value?.createMasterTemplate(parentId)
    }

    suspend fun createSpreadsheet(title: String, parentId: String): String? {
        return _googleApiService.value?.createSpreadsheet(title, parentId)
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String, caseFolderId: String) {
        _googleApiService.value?.attachScript(spreadsheetId, masterTemplateId, caseFolderId)
    }

    fun createCase(caseName: String) {
        viewModelScope.launch {
            val apiService = googleApiService.value ?: return@launch
            val rootFolderId = apiService.getOrCreateAppRootFolder() ?: return@launch
            val masterTemplateId = apiService.createMasterTemplate(rootFolderId) ?: return@launch
            val caseFolderId = apiService.getOrCreateFolder(caseName, rootFolderId) ?: return@launch
            val spreadsheetId = apiService.createSpreadsheet(caseName, caseFolderId) ?: return@launch
            apiService.attachScript(spreadsheetId, masterTemplateId, caseFolderId)
            val newCase = Case(name = caseName, spreadsheetId = spreadsheetId, masterTemplateId = masterTemplateId)
            caseDao.insert(newCase)
        }
    }

    fun selectCase(case: Case) {
        _selectedCase.value = case
        // ...
    }

    private val _extractedText = MutableStateFlow("")
    val extractedText = _extractedText.asStateFlow()

    private val _imageBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val imageBitmap = _imageBitmap.asStateFlow()

    fun onImageSelected(bitmap: android.graphics.Bitmap, context: Context) {
        _imageBitmap.value = bitmap
        viewModelScope.launch {
            processImage(bitmap, context)
        }
    }

    private suspend fun processImage(bitmap: android.graphics.Bitmap, context: Context) {
        Log.d(TAG, "processImage called")
        selectedCase.value?.let { case ->
            val apiService = googleApiService.value ?: return@let
            val rootFolderId = apiService.getOrCreateAppRootFolder() ?: return@let
            val caseFolderId = apiService.getOrCreateFolder(case.name, rootFolderId) ?: return@let
            val rawEvidenceFolderId = apiService.getOrCreateFolder(RAW_EVIDENCE_FOLDER_NAME, caseFolderId) ?: return@let
            val timestamp = System.currentTimeMillis()
            val file = java.io.File(context.cacheDir, "evidence-$timestamp.jpg")
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
            }

            apiService.uploadFile(file, rawEvidenceFolderId, "image/jpeg")
        }

        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "processImage: Text recognition successful")
                _extractedText.value = visionText.text
                val taggedData = DataParser.tagData(visionText.text)
                Log.d(TAG, "processImage: taggedData: $taggedData")
                storeTaggedData(taggedData)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "processImage: Text recognition failed", e)
                _extractedText.value = "Failed to extract text: ${e.message}"
            }
    }

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData = _taggedData.asStateFlow()

    private fun storeTaggedData(taggedData: Map<String, List<String>>) {
        Log.d(TAG, "storeTaggedData called with: $taggedData")
        _taggedData.value = taggedData
            selectedCase.value?.let { case ->
                val spreadsheetId = case.spreadsheetId
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

    fun importSpreadsheet(spreadsheetId: String) {
        viewModelScope.launch {
            val sheetsData = googleApiService.value?.readSpreadsheet(spreadsheetId)
            if (sheetsData != null) {
                val spreadsheetParser = SpreadsheetParser(caseDao, allegationDao, financialEntryDao)
                spreadsheetParser.parseAndStore(sheetsData)
            selectedCase.value?.let { case ->
                val spreadsheetId = case.spreadsheetId
                Log.d(TAG, "storeTaggedData: Storing data to spreadsheet: $spreadsheetId")
                taggedData.forEach { (tag, data) ->
                    if (data.isNotEmpty()) {
                        Log.d(TAG, "storeTaggedData: Adding sheet '$tag' with data: $data")
                        googleApiService.value?.addSheet(spreadsheetId, tag)
                        val values = data.map { listOf(it) }
                        googleApiService.value?.appendData(spreadsheetId, tag, values)
                    }
                }
            }
        }
    }
}

private const val RAW_EVIDENCE_FOLDER_NAME = "Raw Evidence"