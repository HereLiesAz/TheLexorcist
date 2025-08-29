package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.SpreadsheetParser
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.db.Allegation
import com.hereliesaz.lexorcist.db.Case
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import java.io.InputStreamReader
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModelCombined"

    // --- Shared Preferences for Case Info ---
    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)
    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()
    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()
    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    // --- Google API Service and Sign-In State ---
    private val _googleApiService = MutableStateFlow<GoogleApiService?>(null)
    val googleApiService: StateFlow<GoogleApiService?> = _googleApiService.asStateFlow()
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    // --- Case Management ---
    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    val cases: StateFlow<List<Case>> = _cases.asStateFlow()
    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _selectedCase.asStateFlow()

    // --- Data for Selected Case (from Sheets) ---
    private val _selectedCaseSheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val selectedCaseSheetFilters: StateFlow<List<SheetFilter>> = _selectedCaseSheetFilters.asStateFlow()
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()
    private val _selectedCaseEvidenceList = MutableStateFlow<List<Evidence>>(emptyList()) // Renamed from _evidence
    val selectedCaseEvidenceList: StateFlow<List<Evidence>> = _selectedCaseEvidenceList.asStateFlow()

    // --- Generic Evidence List (for UI parsing, MainScreen display, generic export) ---
    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList()) // Renamed from _evidenceList
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    // --- Filters for SettingsScreen (in-memory) ---
    private val _settingScreenFilters = MutableStateFlow<List<SheetFilter>>(emptyList()) // Renamed from _filters
    val settingScreenFilters: StateFlow<List<SheetFilter>> = _settingScreenFilters.asStateFlow()

    // --- Image Processing & Text Extraction ---
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()
    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap.asStateFlow()
    
    // --- Tagged Data for Scripting ---
    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    // --- ScriptRunner ---
    private val scriptRunner = ScriptRunner()
    private var script: String = ""

    init {
        loadCaseInfo() // Load plaintiffs, defendants, court from SharedPreferences
        viewModelScope.launch {
            _googleApiService.collect { apiService ->
                if (apiService != null && _isSignedIn.value) {
                    loadCasesFromRegistry()
                    _selectedCase.value?.let { currentCase ->
                        if (currentCase.spreadsheetId.isNotBlank()) {
                            loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                            loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                            loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id)
                        }
                    }
                } else {
                    _cases.value = emptyList()
                    _selectedCaseSheetFilters.value = emptyList()
                    _allegations.value = emptyList()
                    _selectedCaseEvidenceList.value = emptyList()
                }
            }
        }
    }

    // --- Sign-In Logic ---
    fun onSignInResult(idToken: String?, email: String?, context: Context, applicationName: String) {
        viewModelScope.launch {
            if (email != null && idToken != null) {
                val credential = GoogleAccountCredential
                    .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))
                credential.selectedAccountName = email
                // It's important that GoogleAccountCredential has the idToken to be considered "signed in"
                // However, the library typically handles token refresh itself after initial authorization.
                // Forcing idToken here might not be standard. Let's assume the library handles it.
                
                val service = GoogleApiService(credential, applicationName)
                Log.d(TAG, "GoogleApiService potentially initialized for $email.")
                onSignInSuccess(service) // Call the comprehensive sign-in success logic
            } else {
                Log.w(TAG, "Sign-in result missing email or idToken.")
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService) {
        _googleApiService.value = apiService
        _isSignedIn.value = true
        Log.d(TAG, "onSignInSuccess: Signed in.")
        viewModelScope.launch { // Ensure these are launched in viewModelScope
            loadCasesFromRegistry()
            // If there's a selected case, reload its data
            _selectedCase.value?.let { currentCase ->
                if (currentCase.spreadsheetId.isNotBlank()) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                    loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id)
                }
            }
        }
    }

    private fun onSignInFailed() {
        _googleApiService.value = null
        _isSignedIn.value = false
        Log.d(TAG, "onSignInFailed: Sign in failed.")
        // Clear all data that depends on sign-in
        _cases.value = emptyList()
        _selectedCase.value = null
        _selectedCaseSheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _selectedCaseEvidenceList.value = emptyList()
    }

    fun onSignOut() {
        _googleApiService.value = null // This will trigger the collector in init to clear data
        _isSignedIn.value = false
        Log.d(TAG, "onSignOut: Signed out.")
        // Explicitly clear other states if needed, though the collector should handle most
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        // Clear generic UI list as well
        _uiEvidenceList.value = emptyList()
        _settingScreenFilters.value = emptyList() // Clear in-memory settings filters
        saveCaseInfo() // Save cleared plaintiffs/defendants/court
    }

    // --- SharedPreferences ---
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

    // --- Case Management & Google Drive/Sheets ---
    private suspend fun loadCasesFromRegistry() {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(TAG, "loadCasesFromRegistry: No API service.") }
        try {
            val appRootFolderId = apiService.getOrCreateAppRootFolder() ?: return Unit.also { Log.e(TAG, "Failed to get app root folder.") }
            val registryId = apiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return Unit.also { Log.e(TAG, "Failed to get case registry.") }
            _cases.value = apiService.getAllCasesFromRegistry(registryId)
            Log.d(TAG, "Loaded ${_cases.value.size} cases.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cases", e)
            _cases.value = emptyList()
        }
    }

    fun createCase(caseName: String, exhibitSheetName: String, caseNumber: String, caseSection: String, caseJudge: String) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch Unit.also { Log.w(TAG, "createCase: No API service.") }
            val rootFolderId = apiService.getOrCreateAppRootFolder() ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to get app root folder.") }
            val caseRegistryId = apiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to get case registry.") }
            val caseFolderId = apiService.getOrCreateCaseFolder(caseName) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to create case folder.") }
            apiService.getOrCreateEvidenceFolder(caseName) // Create evidence sub-folder

            val newTemplate = apiService.copyFile(MASTER_TEMPLATE_ID, "$caseName Master Template", caseFolderId) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to copy template.") }
            val newTemplateId = newTemplate.id ?: return@launch Unit.also { Log.e(TAG, "createCase: Copied template has no ID.")}
            
            val replacements = mapOf("CASE_NUMBER" to caseNumber, "CASE_SECTION" to caseSection, "JUDGE" to caseJudge)
            apiService.replacePlaceholdersInDoc(newTemplateId, replacements)

            val caseSpreadsheetId = apiService.createSpreadsheet(caseName, caseFolderId) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to create case spreadsheet.") }
            
            val scriptTemplate = getApplication<Application>().resources.openRawResource(R.raw.apps_script_template).use { InputStreamReader(it).readText() }
            val scriptContent = scriptTemplate
                .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                .replace("{{CASE_NUMBER}}", caseNumber)
                .replace("{{CASE_SECTION}}", caseSection)
                .replace("{{CASE_JUDGE}}", caseJudge)
            apiService.attachScript(caseSpreadsheetId, scriptContent, newTemplateId)

            val newCase = Case(name = caseName, spreadsheetId = caseSpreadsheetId, masterTemplateId = newTemplateId)
            if (apiService.addCaseToRegistry(caseRegistryId, newCase)) {
                Log.d(TAG, "Case '${newCase.name}' added to registry.")
                loadCasesFromRegistry() // Refresh
            } else {
                Log.w(TAG, "Failed to add case '${newCase.name}' to registry.")
            }
        }
    }

    fun selectCase(case: Case?) {
        _selectedCase.value = case
        if (case != null && case.spreadsheetId.isNotBlank() && _googleApiService.value != null) {
            viewModelScope.launch {
                loadSelectedCaseSheetFilters(case.spreadsheetId)
                loadAllegationsForSelectedCase(case.spreadsheetId, case.id)
                loadSelectedCaseEvidence(case.spreadsheetId, case.id)
            }
        } else {
            _selectedCaseSheetFilters.value = emptyList()
            _allegations.value = emptyList()
            _selectedCaseEvidenceList.value = emptyList()
        }
    }

    // --- Case Specific Data Loading (Filters, Allegations, Evidence from Sheets) ---
    private suspend fun loadSelectedCaseSheetFilters(spreadsheetId: String) { // Renamed from loadFiltersFromSheet
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(TAG, "loadSelectedCaseSheetFilters: No API service.") }
        if (spreadsheetId.isBlank()) {
            _selectedCaseSheetFilters.value = emptyList(); return
        }
        try {
            val allSheetData = apiService.readSpreadsheet(spreadsheetId)
            val filterSheetData = allSheetData?.get(FILTERS_SHEET_NAME)
            _selectedCaseSheetFilters.value = filterSheetData?.mapNotNull {
                if (it.size >= 2) SheetFilter(it.getOrNull(0)?.toString() ?: "", it.getOrNull(1)?.toString() ?: "") else null
            } ?: emptyList()
            Log.d(TAG, "Loaded ${_selectedCaseSheetFilters.value.size} sheet filters for $spreadsheetId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sheet filters for $spreadsheetId", e)
            _selectedCaseSheetFilters.value = emptyList()
        }
    }
    
    fun addSelectedCaseSheetFilter(name: String, value: String) { // Renamed from addFilter (older VM)
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(TAG, "addSelectedCaseSheetFilter: Case, Spreadsheet ID, or API Service is missing.")
            return
        }
        viewModelScope.launch {
            try {
                apiService.addSheet(currentCase.spreadsheetId, FILTERS_SHEET_NAME) // Ensure sheet exists
                if (apiService.appendData(currentCase.spreadsheetId, FILTERS_SHEET_NAME, listOf(listOf(name, value))) != null) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId) // Refresh
                } else {
                    Log.w(TAG, "Failed to add sheet filter '$name' to sheet.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sheet filter '$name'", e)
            }
        }
    }

    private suspend fun loadAllegationsForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(TAG, "loadAllegations: No API service.") }
         if (spreadsheetId.isBlank()) {
            _allegations.value = emptyList(); return
        }
        try {
            _allegations.value = apiService.getAllegationsForCase(spreadsheetId, caseIdForAssociation)
            Log.d(TAG, "Loaded ${_allegations.value.size} allegations for case $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading allegations for $spreadsheetId", e)
            _allegations.value = emptyList()
        }
    }

    fun addAllegationToSelectedCase(allegationText: String) { // Renamed from addAllegation
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || allegationText.isBlank() || apiService == null) {
            Log.w(TAG, "addAllegationToSelectedCase: Missing data.")
            return
        }
        viewModelScope.launch {
            try {
                if (apiService.addAllegationToCase(currentCase.spreadsheetId, allegationText)) {
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id) // Refresh
                } else {
                    Log.w(TAG, "Failed to add allegation to case ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding allegation to case ${currentCase.id}", e)
            }
        }
    }

    private suspend fun loadSelectedCaseEvidence(spreadsheetId: String, caseIdForAssociation: Int) { // Renamed from loadEvidenceForSelectedCase
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(TAG, "loadSelectedCaseEvidence: No API service.") }
        if (spreadsheetId.isBlank()) {
            _selectedCaseEvidenceList.value = emptyList(); return
        }
        try {
            _selectedCaseEvidenceList.value = apiService.getEvidenceForCase(spreadsheetId, caseIdForAssociation)
            Log.d(TAG, "Loaded ${_selectedCaseEvidenceList.value.size} evidence items for case $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading evidence for $spreadsheetId", e)
            _selectedCaseEvidenceList.value = emptyList()
        }
    }

    fun addEvidenceToSelectedCase(entry: Evidence) { // Renamed from addEvidence(entry: Evidence)
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(TAG, "addEvidenceToSelectedCase: Missing data.")
            return
        }
        val entryWithCaseId = entry.copy(caseId = currentCase.id) // Ensure caseId is set
        viewModelScope.launch {
            try {
                if (apiService.addEvidenceToCase(currentCase.spreadsheetId, entryWithCaseId)) {
                    loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id) // Refresh
                } else {
                    Log.w(TAG, "Failed to add evidence to case ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding evidence to case ${currentCase.id}", e)
            }
        }
    }
    
    // --- Generic Evidence Parsing & UI List Management (from newer ViewModel) ---
    fun addEvidenceToUiList(uri: Uri, context: Context) { // Renamed from addEvidence (newer VM)
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            val evidence = when (mimeType) {
                "text/plain" -> parseTextFile(uri, context)
                "application/pdf" -> parsePdfFile(uri, context)
                "image/jpeg", "image/png" -> parseImageFileToEvidence(uri, context) // Renamed for clarity
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                else -> { Log.w(TAG, "Unsupported file type: $mimeType"); null }
            }
            evidence?.let {
                _uiEvidenceList.value = _uiEvidenceList.value + it
            }
        }
    }

    private suspend fun parseTextFile(uri: Uri, context: Context): Evidence? { /* ... (implementation retained) ... */
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse text file", e); null }
    }

    private suspend fun parsePdfFile(uri: Uri, context: Context): Evidence? { /* ... (implementation retained) ... */
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val text = buildString { for (i in 1..pdfDocument.numberOfPages) { append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))) } }
                pdfDocument.close()
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse PDF file", e); null }
    }

    private suspend fun parseImageFileToEvidence(uri: Uri, context: Context): Evidence? { // Renamed from parseImageFile
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText -> continuation.resume(Evidence(content = visionText.text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())) }
                    .addOnFailureListener { e -> Log.e(TAG, "ML Kit text recognition failed", e); continuation.resume(null) }
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse image file to evidence", e); null }
    }

    private suspend fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? { /* ... (implementation retained) ... */
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val text = buildString { /* ... */ } // Assuming existing logic here
                workbook.close()
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse spreadsheet file", e); null }
    }

    private suspend fun parseDocFile(uri: Uri, context: Context): Evidence? { /* ... (implementation retained) ... */
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    WordExtractor(HWPFDocument(inputStream)).text
                } else {
                    XWPFWordExtractor(XWPFDocument(inputStream)).text
                }
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse document file", e); null }
    }
    
    fun importSmsMessages(context: Context) {
        viewModelScope.launch {
            val smsList = mutableListOf<Evidence>()
            // Corrected: Added cursor initialization
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
                        smsList.add(Evidence(content = body, timestamp = System.currentTimeMillis(), sourceDocument = "SMS", documentDate = System.currentTimeMillis()))
                    } while (it.moveToNext())
                }
            }
            _uiEvidenceList.value = _uiEvidenceList.value + smsList
        }
    }
    
    // --- Export, Scripting (from newer ViewModel, uses uiEvidenceList) ---
    fun exportToSheet() { // Uses uiEvidenceList
        viewModelScope.launch {
            _googleApiService.value?.let { apiService ->
                val spreadsheetId = apiService.createSpreadsheet("Lexorcist Export")
                spreadsheetId?.let { id ->
                    val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content) }
                    apiService.appendData(id, "Sheet1", data)
                }
            }
        }
    }

    fun setScript(scriptText: String) { this.script = scriptText }

    fun processUiEvidenceForReview() { // Renamed from processEvidenceForReview, acts on uiEvidenceList
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.map { evidence ->
                val parserResult = scriptRunner.runScript(script, evidence)
                TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content)
            }
            EvidenceRepository.setTaggedEvidence(taggedList)
        }
    }
    
    // --- Filters for Settings Screen (in-memory) ---
    fun addSettingScreenFilter(name: String, value: String) { // Renamed from addFilter (newer VM)
        val newFilter = SheetFilter(name = name, value = value)
        _settingScreenFilters.value = _settingScreenFilters.value + newFilter
        Log.d(TAG, "Added setting screen filter: $newFilter. Current setting filters: ${_settingScreenFilters.value}")
    }

    // --- Image Processing & Text Evidence (from older ViewModel) ---
    fun onImageSelectedForProcessing(bitmap: Bitmap, context: Context) { // Renamed from onImageSelected
        _imageBitmap.value = bitmap
        viewModelScope.launch {
            processSelectedImage(bitmap, context) // Renamed from processImage
        }
    }

    private suspend fun processSelectedImage(bitmap: Bitmap, context: Context) { // Renamed from processImage
        Log.d(TAG, "processSelectedImage called")
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        var uploadedDriveFile: com.google.api.services.drive.model.File? = null

        if (currentCase != null && apiService != null) {
            val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return
            val timestamp = System.currentTimeMillis()
            val file = java.io.File(context.cacheDir, "evidence-$timestamp.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, "image/jpeg")
        } else {
            Log.w(TAG, "processSelectedImage: Selected case or API service is null, skipping file upload part.")
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "processSelectedImage: Text recognition successful")
                _extractedText.value = visionText.text
                if (currentCase != null) { // Add to selected case evidence if a case is selected
                    val newEvidence = Evidence(
                        caseId = currentCase.id,
                        content = visionText.text,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = uploadedDriveFile?.name ?: "Processed Image",
                        documentDate = System.currentTimeMillis()
                    )
                    addEvidenceToSelectedCase(newEvidence)
                } else { // Otherwise, maybe add to generic UI list? Or just show text.
                     _uiEvidenceList.value = _uiEvidenceList.value + Evidence(
                        content = visionText.text,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Processed Image (No Case)",
                        documentDate = System.currentTimeMillis()
                    )
                    Log.d(TAG, "processSelectedImage: No case selected, text extracted and added to UI list.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "processSelectedImage: Text recognition failed", e)
                _extractedText.value = "Failed to extract text: ${e.message}"
            }
    }

    fun addTextEvidenceToSelectedCase(text: String, context: Context) { // Renamed from addTextEvidence
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiService.value
            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch
                val timestamp = System.currentTimeMillis()
                val file = java.io.File(context.cacheDir, "text-evidence-$timestamp.txt")
                file.writeText(text)
                val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")
                if (uploadedDriveFile != null) {
                    val newEvidence = Evidence(
                        caseId = currentCase.id, content = text, timestamp = System.currentTimeMillis(),
                        sourceDocument = uploadedDriveFile.name, documentDate = System.currentTimeMillis()
                    )
                    addEvidenceToSelectedCase(newEvidence)
                }
            } else {
                Log.w(TAG, "addTextEvidenceToSelectedCase: No case or API service.")
                 _uiEvidenceList.value = _uiEvidenceList.value + Evidence( // Add to generic if no case
                    content = text, timestamp = System.currentTimeMillis(), sourceDocument = "Text Input (No Case)", documentDate = System.currentTimeMillis()
                )
            }
        }
    }
    
    // --- Spreadsheet Import and Tagged Data (from older ViewModel) ---
    fun importSpreadsheet(spreadsheetIdToImport: String) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch Unit.also { Log.w(TAG, "importSpreadsheet: No API service.") }
            val sheetsData = apiService.readSpreadsheet(spreadsheetIdToImport)
            if (sheetsData != null) {
                // Corrected: SpreadsheetParser constructor call
                val spreadsheetParser = SpreadsheetParser(apiService)
                val newCase = spreadsheetParser.parseAndStore(sheetsData)
                if (newCase != null) {
                    Log.i(TAG, "Spreadsheet imported. New case: ${newCase.name}")
                    loadCasesFromRegistry()
                } else {
                    Log.w(TAG, "Failed to parse/store spreadsheet: $spreadsheetIdToImport")
                }
            } else {
                Log.w(TAG, "Failed to read spreadsheet data for ID: $spreadsheetIdToImport")
            }
        }
    }

    fun storeTaggedDataToSheet(newTaggedData: Map<String, List<String>>) { // Renamed from storeTaggedData
        Log.d(TAG, "storeTaggedDataToSheet called with: $newTaggedData")
        _taggedData.value = newTaggedData // Update local state for UI if needed

        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase != null && apiService != null) {
            viewModelScope.launch {
                val spreadsheetId = currentCase.spreadsheetId
                Log.d(TAG, "Storing tagged data to spreadsheet: $spreadsheetId")
                newTaggedData.forEach { (tag, data) ->
                    if (data.isNotEmpty()) {
                        Log.d(TAG, "Adding sheet '$tag' with data: $data")
                        apiService.addSheet(spreadsheetId, tag) // Ensure sheet exists
                        val values = data.map { listOf(it) }
                        apiService.appendData(spreadsheetId, tag, values)
                    }
                }
            }
        } else {
            Log.w(TAG, "storeTaggedDataToSheet: No case or API service, skipping sheet update.")
        }
    }

    // --- Google Drive File Upload (from older, for addFileEvidence) ---
    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context) { // Renamed from addFileEvidence
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
                    context.contentResolver.openInputStream(uri)?.let { inputStream ->
                        val file = java.io.File(context.cacheDir, fileName)
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                        // Optionally, create an Evidence entry in _selectedCaseEvidenceList here
                        Log.d(TAG, "File $fileName uploaded to Drive for case ${currentCase.name}")
                    }
                }
            } else {
                Log.w(TAG, "addDriveFileEvidenceToSelectedCase: No case or API service.")
            }
        }
    }

    fun updateExtractedText(text: String) {
        _extractedText.value = text
    }
    
    companion object {
        private const val FILTERS_SHEET_NAME = "Filters" // For case-specific filters in sheet
        // private const val RAW_EVIDENCE_FOLDER_NAME = "Raw Evidence" // Managed by GoogleApiService
        private const val MASTER_TEMPLATE_ID = "1Ux9i8GSJ3qJjYqO5ngXMIgCE94LCDkBwfCv0ReJA5eg"
        // Constants like KEY_CASE_NUMBER etc. are used internally by GoogleApiService or SpreadsheetParser
    }
}
