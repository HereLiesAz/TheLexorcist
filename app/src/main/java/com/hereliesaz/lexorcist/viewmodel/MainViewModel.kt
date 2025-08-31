package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.model.File as DriveFile
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.GoogleApiService
import com.google.gson.Gson
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.SpreadsheetParser
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.data.TaggedEvidenceRepository
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.DataParser
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder
import com.hereliesaz.lexorcist.utils.Result
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.content.edit
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: com.hereliesaz.lexorcist.data.EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val googleApiService: GoogleApiService // This injected instance might be unused if _googleApiService stateflow is the primary source
) : AndroidViewModel(application) {

    private val tag = "MainViewModelCombined"

    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)
    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()
    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()
    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _googleApiServiceInternal = MutableStateFlow<GoogleApiService?>(null)
    val googleApiServiceState: StateFlow<GoogleApiService?> = _googleApiServiceInternal.asStateFlow() // Renamed to avoid conflict
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    val cases: StateFlow<List<Case>> = _cases.asStateFlow()
    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _selectedCase.asStateFlow()
    // --- Case Management (Moved to CaseViewModel) ---

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    private val _selectedCaseSheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val selectedCaseSheetFilters: StateFlow<List<SheetFilter>> = _selectedCaseSheetFilters.asStateFlow()
    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()
    private val _selectedCaseEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val selectedCaseEvidenceList: StateFlow<List<Evidence>> = _selectedCaseEvidenceList.asStateFlow()
    // --- Data for Selected Case (Moved to CaseViewModel and EvidenceViewModel) ---

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()
    // --- Generic Evidence List (Moved to EvidenceViewModel) ---

    private val _settingScreenFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val settingScreenFilters: StateFlow<List<SheetFilter>> = _settingScreenFilters.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isUploadingFile = MutableStateFlow(false)
    val isUploadingFile: StateFlow<Boolean> = _isUploadingFile.asStateFlow()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()
    private val _isOcrInProgress = MutableStateFlow(false)
    val isOcrInProgress: StateFlow<Boolean> = _isOcrInProgress.asStateFlow()

    private val _imageBitmapForReview = MutableStateFlow<Bitmap?>(null)
    val imageBitmapForReview: StateFlow<Bitmap?> = _imageBitmapForReview.asStateFlow()
    private var imageUriForReview: Uri? = null

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    private val scriptRunner = ScriptRunner()
    private var script: String = ""

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    init {
        loadCaseInfo()
        loadDarkModePreference()
        viewModelScope.launch {
            _selectedCase.collect { case ->
                if (case != null) {
                    evidenceRepository.getEvidenceForCase(case.spreadsheetId, case.id.toLong()).collect {
                        _selectedCaseEvidenceList.value = it
                    }
                } else {
                    _selectedCaseEvidenceList.value = emptyList()
                }
            }
        }
        viewModelScope.launch {
            _googleApiServiceInternal.collect { apiService ->
                if (apiService != null && _isSignedIn.value) {
                    loadUserData()
                } else {
                    clearUserData(clearGoogleService = false) // Don't clear service itself, just its data
                }
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            loadCasesFromRegistry()
            loadHtmlTemplates()
            _selectedCase.value?.let { currentCase ->
                if (currentCase.spreadsheetId.isNotBlank()) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                }
            }
        }
    }

    private fun clearUserData(clearGoogleService: Boolean = true) {
        if (clearGoogleService) {
            _googleApiServiceInternal.value = null
            GoogleApiServiceHolder.googleApiService = null
        }
        _cases.value = emptyList()
        _htmlTemplates.value = emptyList()
        _selectedCase.value = null // This will trigger selectedCaseEvidenceList to clear via its collector
        _selectedCaseSheetFilters.value = emptyList()
        _allegations.value = emptyList()
        // _selectedCaseEvidenceList is cleared by _selectedCase.collect
    }

    fun onSignInResult(idToken: String?, email: String?, context: Context, applicationName: String) {
        viewModelScope.launch {
            if (email != null && idToken != null) {
                val credential = GoogleAccountCredential
                    .usingOAuth2(context, setOf("https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive.file"))
                credential.selectedAccountName = email
                
                val service = GoogleApiService(credential, applicationName)
                Log.d(tag, "GoogleApiService potentially initialized for $email.")
                onSignInSuccess(service)
            } else {
                Log.w(tag, "Sign-in result missing email or idToken.")
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService) {
        _googleApiServiceInternal.value = apiService
        _isSignedIn.value = true
        GoogleApiServiceHolder.googleApiService = apiService
        Log.d(tag, "onSignInSuccess: Signed in.")
        loadUserData() // Load user data upon successful sign-in
    }

    private fun onSignInFailed() {
        _isSignedIn.value = false
        clearUserData() // Clear all user data and Google service references
        Log.d(tag, "onSignInFailed: Sign in failed.")
    }

    fun onSignOut() {
        _isSignedIn.value = false
        clearUserData() // Clear all user data and Google service references
        Log.d(tag, "onSignOut: Signed out.")
        // Clear local non-Google specific data
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        _uiEvidenceList.value = emptyList()
        _settingScreenFilters.value = emptyList()
        saveCaseInfo() // Persist cleared case info
    }

    // --- SharedPreferences ---
    fun onPlaintiffsChanged(name: String) { _plaintiffs.value = name; saveCaseInfo() }
    fun onDefendantsChanged(name: String) { _defendants.value = name; saveCaseInfo() }
    fun onCourtChanged(name: String) { _court.value = name; saveCaseInfo() }

    private fun saveCaseInfo() {
        sharedPref.edit {
            putString("plaintiffs", _plaintiffs.value)
            putString("defendants", _defendants.value)
            putString("court", _court.value)
        }
    }

    private fun loadCaseInfo() {
        // Moved to CaseViewModel
    }

    private fun loadDarkModePreference() {
        // Moved to CaseViewModel
    }

    fun setDarkMode(isDark: Boolean) {
        // Moved to CaseViewModel
    }

    // --- Case Management & Google Drive/Sheets (Moved to CaseViewModel) ---
    private suspend fun loadCasesFromRegistry() {
        val apiService = _googleApiServiceInternal.value ?: return Unit.also { Log.w(tag, "loadCasesFromRegistry: No API service.") }
        try {
            val appRootFolderId = apiService.getOrCreateAppRootFolder() ?: return Unit.also { Log.e(tag, "Failed to get app root folder.") }
            val registryId = apiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return Unit.also { Log.e(tag, "Failed to get case registry.") }
            _cases.value = apiService.getAllCasesFromRegistry(registryId)
            Log.d(tag, "Loaded ${_cases.value.size} cases.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading cases", e)
            _errorMessage.value = "Error loading cases: ${e.localizedMessage}"
            _cases.value = emptyList()
        }
        // Moved to CaseViewModel
    }

    private suspend fun loadHtmlTemplates() {
        val apiService = _googleApiServiceInternal.value ?: return Unit.also { Log.w(tag, "loadHtmlTemplates: No API service.") }
        try {
            _htmlTemplates.value = apiService.listHtmlTemplatesInAppRootFolder()
            Log.d(tag, "Loaded ${_htmlTemplates.value.size} HTML templates.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading HTML templates", e)
            _errorMessage.value = "Error loading HTML templates: ${e.localizedMessage}"
            _htmlTemplates.value = emptyList()
        }
    }

    private suspend fun generatePdfFromHtmlString(htmlString: String, outputPdfName: String, context: Context): File? = suspendCancellableCoroutine { continuation ->
        Log.d(tag, "generatePdfFromHtmlString for $outputPdfName. HTML length: ${htmlString.length}")
        var webView: WebView? = null
        viewModelScope.launch(Dispatchers.Main) {
            try {
                webView = WebView(context)
                webView?.settings?.javaScriptEnabled = true 
                webView?.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(tag, "WebView onPageFinished for PDF generation.")
                        if (view == null) {
                            Log.e(tag, "WebView instance is null in onPageFinished.")
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }
                        
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        if (printManager == null) {
                            Log.e(tag, "Could not get PrintManager service.")
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }
                        // Actual PDF generation logic needs to be implemented or restored here
                        // For now, resuming with null as the original code was commented out.
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            Log.e(tag, "WebView error (new API): ${error?.errorCode} - ${error?.description} @ ${request.url}")
                            if (continuation.isActive) continuation.resume(null)
                        }
                        super.onReceivedError(view, request, error)
                    }
                }
                webView?.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e(tag, "Exception in generatePdfFromHtmlString WebView setup: ", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
        continuation.invokeOnCancellation {
            viewModelScope.launch(Dispatchers.Main) {
                webView?.destroy()
            }
        }
    }

    fun selectCase(case: Case?) {
        _selectedCase.value = case
        // Data loading for the selected case is now handled by the _googleApiServiceInternal.collect block
        // if the service is available and user is signed in.
        // However, if a case is selected MANUALLY and we are already signed in, we might want to trigger a load.
        if (case != null && case.spreadsheetId.isNotBlank() && _googleApiServiceInternal.value != null && _isSignedIn.value) {
            viewModelScope.launch {
                loadSelectedCaseSheetFilters(case.spreadsheetId)
                loadAllegationsForSelectedCase(case.spreadsheetId, case.id)
            }
        } else if (case == null) { // If case is deselected, clear its specific data
             _selectedCaseSheetFilters.value = emptyList()
             _allegations.value = emptyList()
        }
        // Moved to CaseViewModel
    }

    private suspend fun loadSelectedCaseSheetFilters(spreadsheetId: String) {
        val apiService = _googleApiServiceInternal.value ?: return Unit.also { Log.w(tag, "loadSelectedCaseSheetFilters: No API service.") }
        if (spreadsheetId.isBlank()) {
            _selectedCaseSheetFilters.value = emptyList(); return
        }
        try {
            val allSheetData = apiService.readSpreadsheet(spreadsheetId)
            val filterSheetData = allSheetData?.get(FILTERS_SHEET_NAME)
            _selectedCaseSheetFilters.value = filterSheetData?.mapNotNull {
                if (it.size >= 2) SheetFilter(it.getOrNull(0)?.toString() ?: "", it.getOrNull(1)?.toString() ?: "") else null
            } ?: emptyList()
            Log.d(tag, "Loaded ${_selectedCaseSheetFilters.value.size} sheet filters for $spreadsheetId.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading sheet filters for $spreadsheetId", e)
            _errorMessage.value = "Error loading sheet filters: ${e.localizedMessage}"
            _selectedCaseSheetFilters.value = emptyList()
        }
    }
    
    fun addSelectedCaseSheetFilter(name: String, value: String) {
        val currentCase = _selectedCase.value
        val apiService = _googleApiServiceInternal.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(tag, "addSelectedCaseSheetFilter: Case, Spreadsheet ID, or API Service is missing.")
            _errorMessage.value = "Cannot add filter: Case not selected or not signed in."
            return
        }
        viewModelScope.launch {
            try {
                apiService.addSheet(currentCase.spreadsheetId, FILTERS_SHEET_NAME)
                if (apiService.appendData(currentCase.spreadsheetId, FILTERS_SHEET_NAME, listOf(listOf(name, value))) != null) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                } else {
                    Log.w(tag, "Failed to add sheet filter '$name' to sheet.")
                    _errorMessage.value = "Failed to add filter to sheet."
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding sheet filter '$name'", e)
                _errorMessage.value = "Error adding filter: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun loadAllegationsForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        val apiService = _googleApiServiceInternal.value ?: return Unit.also { Log.w(tag, "loadAllegations: No API service.") }
         if (spreadsheetId.isBlank()) {
            _allegations.value = emptyList(); return
        }
        try {
            _allegations.value = apiService.getAllegationsForCase(spreadsheetId, caseIdForAssociation)
            Log.d(tag, "Loaded ${_allegations.value.size} allegations for case $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading allegations for $spreadsheetId", e)
            _errorMessage.value = "Error loading allegations: ${e.localizedMessage}"
            _allegations.value = emptyList()
        }
    }

    fun addAllegationToSelectedCase(allegationText: String) {
        val currentCase = _selectedCase.value
        val apiService = _googleApiServiceInternal.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || allegationText.isBlank() || apiService == null) {
            Log.w(tag, "addAllegationToSelectedCase: Missing data for adding allegation.")
            _errorMessage.value = "Cannot add allegation: Case not selected, text empty, or not signed in."
            return
        }
        viewModelScope.launch {
            try {
                if (apiService.addAllegationToCase(currentCase.spreadsheetId, allegationText)) {
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                } else {
                    Log.w(tag, "Failed to add allegation to case ${currentCase.id}.")
                    _errorMessage.value = "Failed to add allegation to sheet."
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding allegation to case ${currentCase.id}", e)
                _errorMessage.value = "Error adding allegation: ${e.localizedMessage}"
            }
        }
    }

    fun addEvidenceToSelectedCase(entry: Evidence) {
        // Moved to EvidenceViewModel
    }
    
    fun addEvidenceToUiList(uri: Uri, context: Context) {
        // Moved to EvidenceViewModel
    }

    private suspend fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        // Moved to EvidenceViewModel
        return null
    }

    fun rotateImageBeingReviewed(degrees: Float) {
        // Moved to EvidenceViewModel
    }

    private fun preprocessImageForOcr(bitmap: Bitmap): Bitmap {
        // Moved to EvidenceViewModel
        return bitmap
    }

    fun confirmImageReview(context: Context) {
        // Moved to EvidenceViewModel
    }

    fun cancelImageReview() {
        // Moved to EvidenceViewModel
    }

    private fun parseTextFile(uri: Uri, context: Context): Evidence? {
        // Moved to EvidenceViewModel
        return null
    }

    private fun parsePdfFile(uri: Uri, context: Context): Evidence? {
        // Moved to EvidenceViewModel
        return null
    }

    private fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        // Moved to EvidenceViewModel
        return null
    }

    private fun parseDocFile(uri: Uri, context: Context): Evidence? {
        // Moved to EvidenceViewModel
        return null
    }
    
    fun importSmsMessages(context: Context) {
        // Moved to EvidenceViewModel
        viewModelScope.launch {
            val smsList = mutableListOf<Evidence>()
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.CONTENT_URI,
                null, // Projection
                null, // Selection
                null, // Selection args
                null  // Sort order
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val bodyIndex = it.getColumnIndexOrThrow(android.provider.Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndexOrThrow(android.provider.Telephony.Sms.DATE)
                    do {
                        val body = it.getString(bodyIndex)
                        val dateMillis = it.getLong(dateIndex)
                        smsList.add(
                            Evidence(
                                id = 0,
                                spreadsheetId = _selectedCase.value?.spreadsheetId ?: "",
                                content = body,
                                timestamp = dateMillis, // SMS date is the timestamp
                                sourceDocument = "SMS Message",
                                documentDate = dateMillis, // SMS date as document date
                                allegationId = null,
                                category = "SMS",
                                tags = emptyList()
                            )
                        )
                    } while (it.moveToNext())
                }
            }
            _uiEvidenceList.value = _uiEvidenceList.value + smsList
            if (smsList.isEmpty()) _errorMessage.value = "No SMS messages found or permission denied."
        }
    }
    
    fun exportToSheet() { 
        viewModelScope.launch {
            _googleApiServiceInternal.value?.let { apiService ->
                val spreadsheetId = apiService.createSpreadsheet("Lexorcist Export - ${System.currentTimeMillis()}")
                spreadsheetId?.let { id ->
                    val data = _uiEvidenceList.value.map { evidence ->
                        listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString(), evidence.category, evidence.tags.joinToString(","))
                    }
                    if (data.isNotEmpty()) {
                        apiService.appendData(id, "Sheet1", data)
                        _errorMessage.value = "Exported to new sheet: $id" // Provide some feedback
                    } else {
                         _errorMessage.value = "No data to export."
                    }
                } ?: run {
                    _errorMessage.value = "Failed to create spreadsheet for export."
                }
            } ?: run {
                 _errorMessage.value = "Not signed in. Cannot export to sheet."
            }
        }
    }

    fun setScript(scriptText: String) { this.script = scriptText }

    fun runScriptOnEvidence(evidence: Evidence) {
        viewModelScope.launch {
            val parserResult = scriptRunner.runScript(script, evidence)
            // This function seems to intend to update repository, ensure evidence has a valid DB ID
            // For now, assuming it's for an existing evidence item.
            when (parserResult) {
                 is Result.Success -> {
                    val updatedEvidence = evidence.copy(tags = parserResult.data.tags)
                    evidenceRepository.updateEvidence(updatedEvidence)
                 }
                 is Result.Error -> {
                    _errorMessage.value = "Script execution failed: ${parserResult.exception.message}"
                 }
            }
        }
    }

    fun processUiEvidenceForReview() {
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.mapNotNull { evidence ->
                when (val result = scriptRunner.runScript(script, evidence)) {
                    is Result.Success -> {
                        TaggedEvidence(id = evidence, tags = result.data.tags, content = evidence.content)
                    }
                    is Result.Error -> {
                        _errorMessage.value = "Script execution failed: ${result.exception.message}"
                        null
                    }
                }
            }
            if (taggedList.isNotEmpty()) { // Only set if there's actual tagged evidence
                TaggedEvidenceRepository.setTaggedEvidence(taggedList)
                 _errorMessage.value = "${taggedList.size} evidence items processed for review."
            } else if (_uiEvidenceList.value.isNotEmpty()){
                _errorMessage.value = "No evidence items were successfully processed by the script."
            } else {
                 _errorMessage.value = "No UI evidence to process."
            }
        }
    }
    
    fun addSettingScreenFilter(name: String, value: String) { 
        val newFilter = SheetFilter(name = name, value = value)
        _settingScreenFilters.value = _settingScreenFilters.value + newFilter
        Log.d(tag, "Added setting screen filter: $newFilter. Current setting filters: ${_settingScreenFilters.value}")
    }

    fun addTextEvidenceToSelectedCase(text: String, context: Context) {
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiServiceInternal.value
            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: run {
                    _errorMessage.value = "Could not get or create evidence folder in Drive."
                    return@launch
                }
                val timestamp = System.currentTimeMillis()
                val fileName = "text-evidence-$timestamp.txt"
                val file = File(context.cacheDir, fileName)
                file.writeText(text)

                when (val uploadResult = apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")) {
                    is com.hereliesaz.lexorcist.utils.Result.Success -> {
                        val uploadedDriveFile = uploadResult.data
                        val newEvidence = Evidence(
                            id = 0,
                            spreadsheetId = currentCase.spreadsheetId,
                            content = text,
                            timestamp = timestamp,
                            sourceDocument = uploadedDriveFile.name ?: fileName,
                            documentDate = timestamp,
                            allegationId = null,
                            category = "Text Upload",
                            tags = listOf("drive_upload")
                        )
                        addEvidenceToSelectedCase(newEvidence)
                         _errorMessage.value = "Text evidence uploaded and added."
                    }
                    is com.hereliesaz.lexorcist.utils.Result.Error -> {
                        _errorMessage.value = "Failed to upload text evidence: ${uploadResult.exception.message}"
                    }
                }
                file.delete() // Clean up cache
            } else {
                Log.w(tag, "addTextEvidenceToSelectedCase: No case or API service. Adding to UI list only.")
                 val newEvidenceForUiList = Evidence(
                    id = 0,
                    spreadsheetId = "",
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = "Text Input (Local)",
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Local Text",
                    tags = emptyList()
                )
                _uiEvidenceList.value = _uiEvidenceList.value + newEvidenceForUiList
                _errorMessage.value = "Text added to local list (not signed in or no case selected)."
            }
        }
        // Moved to EvidenceViewModel
    }
    
    fun importSpreadsheet(spreadsheetIdToImport: String) {
        // Moved to CaseViewModel
        viewModelScope.launch {
            val apiService = _googleApiServiceInternal.value ?: return@launch Unit.also {
                Log.w(tag, "importSpreadsheet: No API service.")
                _errorMessage.value = "Cannot import: Not signed in."
            }
            val sheetsData = apiService.readSpreadsheet(spreadsheetIdToImport)
            if (sheetsData != null) {
                val context = getApplication<Application>().applicationContext
                try {
                    val schemaJson = context.resources.openRawResource(R.raw.spreadsheet_schema).bufferedReader().use { it.readText() }
                    val schema = Gson().fromJson(schemaJson, SpreadsheetSchema::class.java)

                    val spreadsheetParser = SpreadsheetParser(apiService, schema)
                    val newCase = spreadsheetParser.parseAndStore(sheetsData)
                    if (newCase != null) {
                        Log.i(tag, "Spreadsheet imported. New case: ${newCase.name}")
                        _errorMessage.value = "Spreadsheet imported: ${newCase.name}"
                        loadCasesFromRegistry() // Refresh case list
                    } else {
                        Log.w(tag, "Failed to parse/store spreadsheet: $spreadsheetIdToImport")
                        _errorMessage.value = "Failed to parse or store spreadsheet data."
                    }
                } catch (e: Exception) {
                     Log.e(tag, "Error during spreadsheet import (schema or parsing): $spreadsheetIdToImport", e)
                    _errorMessage.value = "Error importing spreadsheet: ${e.localizedMessage}"
                }
            } else {
                Log.w(tag, "Failed to read spreadsheet data for ID: $spreadsheetIdToImport")
                _errorMessage.value = "Failed to read spreadsheet data from ID."
            }
        }
    }

    fun storeTaggedDataToSheet(newTaggedData: Map<String, List<String>>) { 
        Log.d(tag, "storeTaggedDataToSheet called with: $newTaggedData")
        _taggedData.value = newTaggedData 

        val currentCase = _selectedCase.value
        val apiService = _googleApiServiceInternal.value
        if (currentCase != null && apiService != null) {
            viewModelScope.launch {
                val spreadsheetId = currentCase.spreadsheetId
                Log.d(tag, "Storing tagged data to spreadsheet: $spreadsheetId")
                var successCount = 0
                newTaggedData.forEach { (sheetName, data) -> // Changed tag to sheetName for clarity
                    if (data.isNotEmpty()) {
                        Log.d(tag, "Adding/Updating sheet '$sheetName' with data count: ${data.size}")
                        apiService.addSheet(spreadsheetId, sheetName)
                        val values = data.map { listOf(it) }
                        if (apiService.appendData(spreadsheetId, sheetName, values) != null) {
                            successCount++
                        }
                    }
                }
                _errorMessage.value = "Stored $successCount tag groups to sheet."
            }
        } else {
            Log.w(tag, "storeTaggedDataToSheet: No case or API service, skipping sheet update.")
            _errorMessage.value = "Cannot store to sheet: No case selected or not signed in."
        }
    }

    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context) {
        // Moved to EvidenceViewModel
    }

    fun updateExtractedText(text: String) {
        _extractedText.value = text
    }

    // --- Evidence Editing (Moved to EvidenceViewModel) ---
    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
    }

    fun uploadAudioFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            // Similar logic to addDriveFileEvidenceToSelectedCase, but specific for audio
            // For now, this is a simplified version. Consider merging with addDriveFileEvidence.
            val currentCase = _selectedCase.value
            val apiService = _googleApiServiceInternal.value
            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"
                    var localFileToClean : File? = null
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val file = File(context.cacheDir, fileName)
                            localFileToClean = file
                            file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                            when (apiService.uploadFile(file, rawEvidenceFolderId, mimeType)) {
                                is com.hereliesaz.lexorcist.utils.Result.Success -> {
                                     _errorMessage.value = "Audio file '$fileName' uploaded."
                                     // Optionally, create an Evidence entry here as well
                                }
                                is com.hereliesaz.lexorcist.utils.Result.Error -> {
                                     _errorMessage.value = "Failed to upload audio file '$fileName'."
                                }
                            }
                        }
                    } catch(e: Exception) {
                        _errorMessage.value = "Error uploading audio: ${e.localizedMessage}"
                    } finally {
                        localFileToClean?.delete()
                    }
                }
            } else {
                 _errorMessage.value = "Cannot upload audio: No case or not signed in."
            }
        }
    }

    companion object {
        private const val FILTERS_SHEET_NAME = "Filters" 
    }
}
