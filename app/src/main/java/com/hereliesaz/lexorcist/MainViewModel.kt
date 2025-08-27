package com.hereliesaz.lexorcist

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// import com.hereliesaz.lexorcist.db.AppDatabase // Removed unused import
import com.hereliesaz.lexorcist.db.Allegation
import com.hereliesaz.lexorcist.db.Case
import com.hereliesaz.lexorcist.db.FinancialEntry
import com.hereliesaz.lexorcist.model.SheetFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // private val financialEntryDao = db.financialEntryDao() // Removed financialEntryDao

    private val TAG = "MainViewModel"

    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _plaintiffs = MutableStateFlow("")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow("")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow("")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    val cases: StateFlow<List<Case>> = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _selectedCase.asStateFlow()

    private val _financialEntries = MutableStateFlow<List<FinancialEntry>>(emptyList())
    val financialEntries: StateFlow<List<FinancialEntry>> = _financialEntries.asStateFlow()

    private val _filters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val filters: StateFlow<List<SheetFilter>> = _filters.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()

    init {
        loadCaseInfo()
        viewModelScope.launch {
            _googleApiService.collect { apiService ->
                if (apiService != null && _isSignedIn.value) {
                    loadCasesFromRegistry()
                    _selectedCase.value?.let { currentCase ->
                        if (currentCase.spreadsheetId.isNotBlank()) {
                            loadFiltersFromSheet(currentCase.spreadsheetId)
                            loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                            loadFinancialEntriesForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                        }
                    }
                } else {
                    // Clear data if API service is null or not signed in
                    _cases.value = emptyList()
                    _filters.value = emptyList()
                    _allegations.value = emptyList()
                    _financialEntries.value = emptyList()
                }
            }
        }
    }

    fun addFileEvidence(uri: Uri, context: Context) {
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiService.value

            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    val mimeType = context.contentResolver.getType(uri)

                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val file = java.io.File(context.cacheDir, fileName)
                        file.outputStream().use {
                            inputStream.copyTo(it)
                        }
                        apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                    }
                }
            } else {
                Log.w(TAG, "addFileEvidence: Selected case or API service is null, skipping file upload.")
            }
        }
    }

    private suspend fun loadCasesFromRegistry() {
        val apiService = _googleApiService.value
        if (apiService == null) {
            Log.w(TAG, "loadCasesFromRegistry: GoogleApiService is null.")
            _cases.value = emptyList(); return
        }
        try {
            val appRootFolderId = apiService.getOrCreateAppRootFolder()
            if (appRootFolderId == null) {
                Log.e(TAG, "loadCasesFromRegistry: Failed to get app root folder ID.")
                _cases.value = emptyList(); return
            }
            val registryId = apiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
            if (registryId == null) {
                Log.e(TAG, "loadCasesFromRegistry: Failed to get case registry spreadsheet ID.")
                _cases.value = emptyList(); return
            }
            _cases.value = apiService.getAllCasesFromRegistry(registryId)
            Log.d(TAG, "loadCasesFromRegistry: Loaded ${_cases.value.size} cases.")
        } catch (e: Exception) {
            Log.e(TAG, "loadCasesFromRegistry: Error loading cases", e)
            _cases.value = emptyList()
        }
    }

    private suspend fun loadFiltersFromSheet(spreadsheetId: String) {
        val apiService = _googleApiService.value
        if (apiService == null || spreadsheetId.isBlank()) {
            _filters.value = emptyList(); return
        }
        try {
            val allSheetData = apiService.readSpreadsheet(spreadsheetId)
            val filterSheetData = allSheetData?.get(FILTERS_SHEET_NAME)
            _filters.value = filterSheetData?.mapNotNull {
                if (it.size >= 2) SheetFilter(it.getOrNull(0)?.toString() ?: "", it.getOrNull(1)?.toString() ?: "") else null
            } ?: emptyList()
            Log.d(TAG, "loadFiltersFromSheet: Loaded ${_filters.value.size} filters.")
        } catch (e: Exception) {
            Log.e(TAG, "loadFiltersFromSheet: Error loading filters for $spreadsheetId", e)
            _filters.value = emptyList()
        }
    }

    private suspend fun loadAllegationsForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        val apiService = _googleApiService.value
        if (apiService == null || spreadsheetId.isBlank()) {
            _allegations.value = emptyList(); return
        }
        try {
            _allegations.value = apiService.getAllegationsForCase(spreadsheetId, caseIdForAssociation)
            Log.d(TAG, "loadAllegationsForSelectedCase: Loaded ${_allegations.value.size} allegations for case ID $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(TAG, "loadAllegationsForSelectedCase: Error loading allegations for $spreadsheetId", e)
            _allegations.value = emptyList()
        }
    }

    private suspend fun loadFinancialEntriesForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        val apiService = _googleApiService.value
        if (apiService == null || spreadsheetId.isBlank()) {
            _financialEntries.value = emptyList(); return
        }
        try {
            _financialEntries.value = apiService.getFinancialEntriesForCase(spreadsheetId, caseIdForAssociation)
            Log.d(TAG, "loadFinancialEntriesForSelectedCase: Loaded ${_financialEntries.value.size} financial entries for case ID $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(TAG, "loadFinancialEntriesForSelectedCase: Error loading financial entries for $spreadsheetId", e)
            _financialEntries.value = emptyList()
        }
    }

    fun onPlaintiffsChanged(name: String) { _plaintiffs.value = name; saveCaseInfo() }
    fun onDefendantsChanged(name: String) { _defendants.value = name; saveCaseInfo() }
    fun onCourtChanged(name: String) { _court.value = name; saveCaseInfo() }

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
        viewModelScope.launch {
            loadCasesFromRegistry()
            _selectedCase.value?.let { currentCase ->
                if (currentCase.spreadsheetId.isNotBlank()) {
                    loadFiltersFromSheet(currentCase.spreadsheetId)
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                    loadFinancialEntriesForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                }
            }
        }
    }

    fun onSignInFailed() {
        _googleApiService.value = null
        _isSignedIn.value = false
        _cases.value = emptyList()
        _filters.value = emptyList()
        _allegations.value = emptyList()
        _financialEntries.value = emptyList()
    }

    fun onSignOut() {
        _googleApiService.value = null
        _isSignedIn.value = false
        _cases.value = emptyList()
        _selectedCase.value = null
        _filters.value = emptyList()
        _allegations.value = emptyList()
        _financialEntries.value = emptyList()
    }

    fun createCase(
        caseName: String,
        caseNumber: String,
        caseSection: String,
        judge: String,
        plaintiffs: String,
        defendants: String,
        courtName: String,
        courtDistrict: String
    ) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch
            val rootFolderId = apiService.getOrCreateAppRootFolder() ?: return@launch
            val caseRegistrySpreadsheetId = apiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return@launch

            val caseFolderId = apiService.getOrCreateCaseFolder(caseName) ?: return@launch
            apiService.getOrCreateEvidenceFolder(caseName) // Create evidence folder when case is created
            val caseSpreadsheetId = apiService.createSpreadsheet(caseName, caseFolderId) ?: return@launch

            val caseInfo = mapOf(
                "Case Number" to caseNumber,
                "Section" to caseSection,
                "Judge" to judge,
                "Plaintiffs" to plaintiffs,
                "Defendants" to defendants,
                "Court Name" to courtName,
                "Court District" to courtDistrict
            )
            apiService.addCaseInfoSheet(caseSpreadsheetId, caseInfo)

            val scriptTemplate = getApplication<Application>().resources.openRawResource(R.raw.apps_script_template)
                .bufferedReader().use { it.readText() }
            apiService.attachScript(caseSpreadsheetId, scriptTemplate)

            val newCase = Case(name = caseName, spreadsheetId = caseSpreadsheetId)
            if (apiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCase)) {
                Log.d(TAG, "createCase: Case '$caseName' added to registry.")
                loadCasesFromRegistry()
            } else {
                Log.w(TAG, "createCase: Failed to add case '$caseName' to registry.")
            }
        }
    }

    fun selectCase(case: Case?) {
        _selectedCase.value = case
        if (case != null && case.spreadsheetId.isNotBlank() && _googleApiService.value != null) {
            viewModelScope.launch {
                loadFiltersFromSheet(case.spreadsheetId)
                loadAllegationsForSelectedCase(case.spreadsheetId, case.id)
                loadFinancialEntriesForSelectedCase(case.spreadsheetId, case.id)
            }
        } else {
            _filters.value = emptyList()
            _allegations.value = emptyList()
            _financialEntries.value = emptyList()
        }
    }

    fun addFilter(name: String, value: String) {
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) return

        viewModelScope.launch {
            try {
                apiService.addSheet(currentCase.spreadsheetId, FILTERS_SHEET_NAME) 
                if (apiService.appendData(currentCase.spreadsheetId, FILTERS_SHEET_NAME, listOf(listOf(name, value))) != null) {
                    loadFiltersFromSheet(currentCase.spreadsheetId)
                } else {
                    Log.w(TAG, "addFilter: Failed to add filter '$name' to sheet.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "addFilter: Error adding filter '$name'", e)
            }
        }
    }

    fun addAllegation(allegationText: String) {
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || allegationText.isBlank() || apiService == null) {
            Log.w(TAG, "addAllegation: Missing data for adding allegation.")
            return
        }
        viewModelScope.launch {
            try {
                if (apiService.addAllegationToCase(currentCase.spreadsheetId, allegationText)) {
                    Log.d(TAG, "addAllegation: Allegation added for case ID ${currentCase.id}.")
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                } else {
                    Log.w(TAG, "addAllegation: Failed to add allegation for case ID ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "addAllegation: Error adding allegation for case ID ${currentCase.id}", e)
            }
        }
    }

    fun addFinancialEntry(entry: FinancialEntry) {
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(TAG, "addFinancialEntry: Missing data for adding financial entry.")
            return
        }
        // Ensure the entry is associated with the selected case ID
        val entryWithCaseId = entry.copy(caseId = currentCase.id)
        viewModelScope.launch {
            try {
                if (apiService.addFinancialEntryToCase(currentCase.spreadsheetId, entryWithCaseId)) {
                    Log.d(TAG, "addFinancialEntry: Entry added for case ID ${currentCase.id}.")
                    loadFinancialEntriesForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                } else {
                    Log.w(TAG, "addFinancialEntry: Failed to add entry for case ID ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "addFinancialEntry: Error adding entry for case ID ${currentCase.id}", e)
            }
        }
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

    fun addTextEvidence(text: String, context: Context) {
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiService.value

            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch
                val timestamp = System.currentTimeMillis()
                val file = java.io.File(context.cacheDir, "evidence-$timestamp.txt")
                file.writeText(text)
                apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")
            } else {
                Log.w(TAG, "addTextEvidence: Selected case or API service is null, skipping file upload.")
            }
        }
    }

    private suspend fun processImage(bitmap: android.graphics.Bitmap, context: Context) {
        Log.d(TAG, "processImage called")
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value

        if (currentCase != null && apiService != null) {
            val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return
            val timestamp = System.currentTimeMillis()
            val file = java.io.File(context.cacheDir, "evidence-$timestamp.jpg")
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
            }
            apiService.uploadFile(file, rawEvidenceFolderId, "image/jpeg")
        } else {
            Log.w(TAG, "processImage: Selected case or API service is null, skipping file upload.")
        }

        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "processImage: Text recognition successful")
                _extractedText.value = visionText.text
                val taggedDataMap = DataParser.tagData(visionText.text)
                Log.d(TAG, "processImage: taggedData: $taggedDataMap")
                storeTaggedData(taggedDataMap)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "processImage: Text recognition failed", e)
                _extractedText.value = "Failed to extract text: ${e.message}"
            }
    }

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    private fun storeTaggedData(newTaggedData: Map<String, List<String>>) {
        Log.d(TAG, "storeTaggedData called with: $newTaggedData")
        _taggedData.value = newTaggedData
        
        val currentCase = _selectedCase.value
        val currentApiService = _googleApiService.value

        if (currentCase != null && currentApiService != null) {
            viewModelScope.launch { 
                val spreadsheetId = currentCase.spreadsheetId
                Log.d(TAG, "storeTaggedData: Storing data to spreadsheet: $spreadsheetId")
                newTaggedData.forEach { (tag, data) ->
                    if (data.isNotEmpty()) {
                        Log.d(TAG, "storeTaggedData: Adding sheet '$tag' with data: $data")
                        currentApiService.addSheet(spreadsheetId, tag)
                        val values = data.map { listOf(it) }
                        currentApiService.appendData(spreadsheetId, tag, values)
                    }
                }
            }
        } else {
            Log.w(TAG, "storeTaggedData: Selected case or API service is null, skipping sheet update.")
        }
    }

    fun importSpreadsheet(spreadsheetIdToImport: String) { 
        viewModelScope.launch {
            val currentApiService = _googleApiService.value
            if (currentApiService == null) {
                Log.w(TAG, "importSpreadsheet: GoogleApiService is null.")
                return@launch
            }

            val sheetsData = currentApiService.readSpreadsheet(spreadsheetIdToImport)
            if (sheetsData != null) {
                val spreadsheetParser = SpreadsheetParser(currentApiService) // Pass GoogleApiService
                val newCase = spreadsheetParser.parseAndStore(sheetsData)
                if (newCase != null) {
                    Log.i(TAG, "Spreadsheet data imported successfully. New case: ${newCase.name}")
                    loadCasesFromRegistry() // Refresh the case list
                } else {
                    Log.w(TAG, "importSpreadsheet: Failed to parse and store spreadsheet data, or case already exists. Spreadsheet ID: $spreadsheetIdToImport")
                    // In a real app, notify the user (e.g., Toast: "Import failed or case already exists.")
                }
            } else {
                Log.w(TAG, "importSpreadsheet: Failed to read spreadsheet data for ID: $spreadsheetIdToImport")
            }
        }
    }

    companion object {
        private const val FILTERS_SHEET_NAME = "Filters"
        private const val RAW_EVIDENCE_FOLDER_NAME = "Raw Evidence"
        // ALLEGATIONS_SHEET_NAME is in GoogleApiService.kt
        // FINANCIAL_ENTRIES_SHEET_NAME is in GoogleApiService.kt
    }
}
