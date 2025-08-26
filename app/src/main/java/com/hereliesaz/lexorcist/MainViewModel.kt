package com.hereliesaz.lexorcist

import android.app.Application
import android.content.Context
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

    private val financialEntryDao = AppDatabase.getDatabase(application).financialEntryDao()
    private val caseDao = AppDatabase.getDatabase(application).caseDao()
    // private val filterDao = AppDatabase.getDatabase(application).filterDao()

    val financialEntries = financialEntryDao.getAllEntries()

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    val cases = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase = _selectedCase.asStateFlow()

    // private val _filters = MutableStateFlow<List<Filter>>(emptyList())
    // val filters = _filters.asStateFlow()

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

    suspend fun createMasterTemplate(): String? {
        return _googleApiService.value?.createMasterTemplate()
    }

    suspend fun createSpreadsheet(title: String, caseInfo: Map<String, String>): String? {
        return _googleApiService.value?.createSpreadsheet(title, caseInfo)
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String) {
        _googleApiService.value?.attachScript(spreadsheetId, masterTemplateId)
    }

    fun createCase(caseName: String) {
        viewModelScope.launch {
            val caseInfo = mapOf(
                "plaintiffs" to plaintiffs.value,
                "defendants" to defendants.value,
                "court" to court.value
            )
            val spreadsheetId = googleApiService.value?.createSpreadsheet(caseName, caseInfo)
            if (spreadsheetId != null) {
                val newCase = Case(name = caseName, spreadsheetId = spreadsheetId)
                caseDao.insert(newCase)
            }
        }
    }

    fun selectCase(case: Case) {
        _selectedCase.value = case
        // viewModelScope.launch {
        //     filterDao.getFiltersForCase(case.id).collect {
        //         _filters.value = it
        //     }
        // }
    }

    // fun addFilter(name: String, value: String) {
    //     selectedCase.value?.let {
    //         viewModelScope.launch {
    //             val filter = Filter(caseId = it.id, name = name, value = value)
    //             filterDao.insert(filter)
    //         }
    //     }
    // }

    private val _extractedText = MutableStateFlow("")
    val extractedText = _extractedText.asStateFlow()

    private val _imageBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val imageBitmap = _imageBitmap.asStateFlow()

    fun onImageSelected(bitmap: android.graphics.Bitmap) {
        _imageBitmap.value = bitmap
        processImage(bitmap)
    }

    private fun processImage(bitmap: android.graphics.Bitmap) {
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                _extractedText.value = visionText.text
                val taggedData = DataParser.tagData(visionText.text)
                storeTaggedData(taggedData)
            }
            .addOnFailureListener { e ->
                _extractedText.value = "Failed to extract text: ${e.message}"
            }
    }

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData = _taggedData.asStateFlow()

    private fun storeTaggedData(taggedData: Map<String, List<String>>) {
        _taggedData.value = taggedData
        viewModelScope.launch {
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
}