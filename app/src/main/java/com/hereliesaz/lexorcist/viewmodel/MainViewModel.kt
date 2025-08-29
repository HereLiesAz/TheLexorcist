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
import android.provider.Settings.Global.putString
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.model.File as DriveFile // Added import alias
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.SpreadsheetParser
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.EvidenceRepository

import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.GoogleApiServiceHolder
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

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

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

    // --- HTML Templates ---
    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

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

    // --- Error Handling ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- Image Processing & Text Extraction ---
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()
    private val _isOcrInProgress = MutableStateFlow(false)
    val isOcrInProgress: StateFlow<Boolean> = _isOcrInProgress.asStateFlow()

    // --- Image Review Workflow ---
    private val _imageBitmapForReview = MutableStateFlow<Bitmap?>(null)
    val imageBitmapForReview: StateFlow<Bitmap?> = _imageBitmapForReview.asStateFlow()
    private var imageUriForReview: Uri? = null
    
    // --- Tagged Data for Scripting ---
    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    // --- ScriptRunner ---
    private val scriptRunner = ScriptRunner()
    private var script: String = ""

    init {
        loadCaseInfo() // Load plaintiffs, defendants, court from SharedPreferences
        loadDarkModePreference()
        viewModelScope.launch {
            _googleApiService.collect { apiService ->
                if (apiService != null && _isSignedIn.value) {
                    loadCasesFromRegistry()
                    loadHtmlTemplates() // Load HTML templates
                    _selectedCase.value?.let { currentCase ->
                        if (currentCase.spreadsheetId.isNotBlank()) {
                            loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                            loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                            loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id)
                        }
                    }
                } else {
                    _cases.value = emptyList()
                    _htmlTemplates.value = emptyList() // Clear templates if not signed in or no API service
                    _selectedCaseSheetFilters.value = emptyList()
                    _allegations.value = emptyList()
                    _selectedCaseEvidenceList.value = emptyList()
                }
            }
        }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiService.value
            if (currentCase != null && apiService != null) {
                if (apiService.updateEvidenceInCase(currentCase.spreadsheetId, evidence)) {
                    loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id) // Reload
                } else {
                    showError("Failed to update evidence.")
                    Log.w(TAG, "updateEvidence: Failed to update evidence in sheet.")
                }
            } else {
                Log.w(TAG, "updateEvidence: Missing data.")
            }
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch {
            val currentCase = _selectedCase.value
            val apiService = _googleApiService.value
            if (currentCase != null && apiService != null) {
                if (apiService.deleteEvidenceFromCase(currentCase.spreadsheetId, evidence.id)) {
                    loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id) // Reload
                } else {
                    showError("Failed to delete evidence.")
                    Log.w(TAG, "deleteEvidence: Failed to delete evidence from sheet.")
                }
            } else {
                Log.w(TAG, "deleteEvidence: Missing data.")
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
                
                val service = GoogleApiService(credential, applicationName)
                Log.d(TAG, "GoogleApiService potentially initialized for $email.")
                onSignInSuccess(service)
            } else {
                Log.w(TAG, "Sign-in result missing email or idToken.")
                onSignInFailed()
            }
        }
    }

    private fun onSignInSuccess(apiService: GoogleApiService) {
        _googleApiService.value = apiService
        _isSignedIn.value = true
        GoogleApiServiceHolder.googleApiService = apiService
        Log.d(TAG, "onSignInSuccess: Signed in.")
        viewModelScope.launch { 
            loadCasesFromRegistry()
            loadHtmlTemplates() // Load HTML templates
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
        _cases.value = emptyList()
        _htmlTemplates.value = emptyList() // Clear templates
        _selectedCase.value = null
        _selectedCaseSheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _selectedCaseEvidenceList.value = emptyList()
    }

    fun onSignOut() {
        _googleApiService.value = null 
        _isSignedIn.value = false
        GoogleApiServiceHolder.googleApiService = null
        Log.d(TAG, "onSignOut: Signed out.")
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        _cases.value = emptyList()
        _htmlTemplates.value = emptyList() // Clear templates
        _selectedCase.value = null
        _selectedCaseSheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _selectedCaseEvidenceList.value = emptyList()
        _uiEvidenceList.value = emptyList()
        _settingScreenFilters.value = emptyList()
        saveCaseInfo()
        cancelImageReview() // Clear any image under review
    }

    // --- SharedPreferences ---
    fun onPlaintiffsChanged(name: String) { _plaintiffs.value = name; saveCaseInfo() }
    fun onDefendantsChanged(name: String) { _defendants.value = name; saveCaseInfo() }
    fun onCourtChanged(name: String) { _court.value = name; saveCaseInfo() }

    private fun saveCaseInfo() {
        sharedPref.edit() {
            putString("plaintiffs", _plaintiffs.value)
            putString("defendants", _defendants.value)
            putString("court", _court.value)
        }
    }

    private fun loadCaseInfo() {
        _plaintiffs.value = sharedPref.getString("plaintiffs", "") ?: ""
        _defendants.value = sharedPref.getString("defendants", "") ?: ""
        _court.value = sharedPref.getString("court", "") ?: ""
    }

    private fun loadDarkModePreference() {
        _isDarkMode.value = sharedPref.getBoolean("is_dark_mode", false)
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        sharedPref.edit() {
            putBoolean("is_dark_mode", isDark)
        }
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

    private suspend fun loadHtmlTemplates() {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(TAG, "loadHtmlTemplates: No API service.") }
        try {
            _htmlTemplates.value = apiService.listHtmlTemplatesInAppRootFolder()
            Log.d(TAG, "Loaded ${_htmlTemplates.value.size} HTML templates.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading HTML templates", e)
            _htmlTemplates.value = emptyList()
        }
    }

    private suspend fun generatePdfFromHtmlString(htmlString: String, outputPdfName: String, context: Context): File? = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "generatePdfFromHtmlString for $outputPdfName. HTML length: ${htmlString.length}")

        val outputPdfFile = File(context.cacheDir, outputPdfName)
        var webView: WebView? = null

        viewModelScope.launch(Dispatchers.Main) { // WebView operations must be on the Main thread
            try {
                webView = WebView(context)
                webView?.settings?.javaScriptEnabled = true // Enable if HTML uses JS, otherwise optional
                webView?.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "WebView onPageFinished for PDF generation.")
                        if (view == null) {
                            Log.e(TAG, "WebView instance is null in onPageFinished.")
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }
                        
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        if (printManager == null) {
                            Log.e(TAG, "Could not get PrintManager service.")
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }

                        val jobName = "${context.packageName} Document"

                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()

                        // Fallback: resume with null since the direct PDF generation (using PrintDocumentAdapter) is currently commented out
                        if (continuation.isActive) continuation.resume(null)
                    }

                    // The deprecated onReceivedError(view, errorCode, description, failingUrl) has been removed.

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            Log.e(TAG, "WebView error (new API): ${error?.errorCode} - ${error?.description} @ ${request.url}")
                            if (continuation.isActive) continuation.resume(null)
                        }
                        super.onReceivedError(view, request, error) // Still call super if needed, or handle exclusively.
                    }
                }
                webView?.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in generatePdfFromHtmlString WebView setup: ", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
        continuation.invokeOnCancellation {
            viewModelScope.launch(Dispatchers.Main) {
                webView?.destroy()
            }
        }
    }

    fun createCase(
        caseName: String, 
        exhibitSheetName: String, 
        caseNumber: String, 
        caseSection: String, 
        caseJudge: String,
        selectedMasterHtmlTemplateId: String // Added new parameter
    ) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch Unit.also { Log.w(TAG, "createCase: No API service.") }
            val applicationContext = getApplication<Application>().applicationContext

            val rootFolderId = apiService.getOrCreateAppRootFolder() ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to get app root folder.") }
            val caseRegistryId = apiService.getOrCreateCaseRegistrySpreadsheetId(rootFolderId) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to get case registry.") }
            val caseFolderId = apiService.getOrCreateCaseFolder(caseName) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to create case folder for '$caseName'.") }
            apiService.getOrCreateEvidenceFolder(caseName) // Create evidence sub-folder

            // 1. Download HTML Template using selectedMasterHtmlTemplateId
            val htmlTemplateContent = apiService.downloadFileAsString(selectedMasterHtmlTemplateId)
            if (htmlTemplateContent == null) {
                Log.e(TAG, "createCase: Failed to download HTML template (ID: $selectedMasterHtmlTemplateId).")
                return@launch
            }
            Log.d(TAG, "createCase: HTML template (ID: $selectedMasterHtmlTemplateId) downloaded successfully.")

            // 2. Replace Placeholders in HTML
            var processedHtml = htmlTemplateContent
                .replace("{{CASE_NAME}}", caseName) 
                .replace("{{CASE_NUMBER}}", caseNumber)
                .replace("{{CASE_SECTION}}", caseSection)
                .replace("{{JUDGE}}", caseJudge)
            processedHtml = processedHtml
                .replace("{{PLAINTIFFS}}", _plaintiffs.value)
                .replace("{{DEFENDANTS}}", _defendants.value)
                .replace("{{COURT}}", _court.value)
            Log.d(TAG, "createCase: Placeholders replaced in HTML.")

            // 2.5 Upload Processed HTML as a Snapshot
            val snapshotHtmlFileName = "${caseName}_Snapshot.html"
            val uploadedHtmlSnapshot = apiService.uploadStringAsFile(processedHtml, "text/html", snapshotHtmlFileName, caseFolderId)
            val snapshotHtmlId = uploadedHtmlSnapshot?.id
            if (snapshotHtmlId == null) {
                Log.w(TAG, "createCase: Failed to upload HTML snapshot for case '$caseName'. Proceeding without it.")
            } else {
                Log.d(TAG, "createCase: HTML snapshot uploaded successfully. ID: $snapshotHtmlId")
            }

            // 3. Convert Processed HTML to PDF (locally)
            val outputPdfName = "$caseName.pdf"
            val localPdfFile = generatePdfFromHtmlString(processedHtml, outputPdfName, applicationContext)
            
            if (localPdfFile == null) {
                Log.e(TAG, "createCase: Failed to generate local PDF from HTML.")
            }
            Log.d(TAG, "createCase: Local PDF generation completed. File: ${localPdfFile?.absolutePath ?: "null"}")

            // 4. Upload the generated PDF to Google Drive
            var generatedPdfId: String? = null
            if (localPdfFile != null && localPdfFile.exists()) {
                val uploadedPdfDriveFile = apiService.uploadFile(localPdfFile, caseFolderId, "application/pdf")
                if (uploadedPdfDriveFile == null) {
                    Log.e(TAG, "createCase: Failed to upload generated PDF to Drive folder $caseFolderId.")
                } else {
                    generatedPdfId = uploadedPdfDriveFile.id
                    Log.d(TAG, "createCase: Generated PDF uploaded successfully. ID: $generatedPdfId")
                    localPdfFile.delete() // Clean up local cache
                }
            } else {
                Log.w(TAG, "createCase: Local PDF file is null or does not exist, skipping PDF upload.")
            }

            // 5. Create Case Spreadsheet
            val caseSpreadsheetId = apiService.createSpreadsheet(caseName, caseFolderId) ?: return@launch Unit.also { Log.e(TAG, "createCase: Failed to create case spreadsheet for '$caseName'.") }
            Log.d(TAG, "createCase: Case spreadsheet created. ID: $caseSpreadsheetId")
            
            val scriptTemplate = getApplication<Application>().resources.openRawResource(R.raw.apps_script_template).use { InputStreamReader(it).readText() }
            val scriptContent = scriptTemplate
                .replace("{{EXHIBIT_SHEET_NAME}}", exhibitSheetName)
                .replace("{{CASE_NUMBER}}", caseNumber)
                .replace("{{CASE_SECTION}}", caseSection)
                .replace("{{CASE_JUDGE}}", caseJudge)
            
            apiService.attachScript(caseSpreadsheetId, scriptContent, generatedPdfId ?: "") 
            Log.d(TAG, "createCase: Script attached to spreadsheet. Master Doc ID used for script: ${generatedPdfId ?: "(none)"}")

            val newCase = Case(
                name = caseName,
                spreadsheetId = caseSpreadsheetId,
                generatedPdfId = generatedPdfId,
                sourceHtmlSnapshotId = snapshotHtmlId,
                originalMasterHtmlTemplateId = selectedMasterHtmlTemplateId
            )
            if (apiService.addCaseToRegistry(caseRegistryId, newCase)) {
                Log.d(TAG, "Case '${newCase.name}' added to registry. PDF ID: ${newCase.generatedPdfId}, HTML Snapshot ID: ${newCase.sourceHtmlSnapshotId}, Original Template: ${newCase.originalMasterHtmlTemplateId}.")
                loadCasesFromRegistry() 
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

    private suspend fun loadSelectedCaseSheetFilters(spreadsheetId: String) { 
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
    
    fun addSelectedCaseSheetFilter(name: String, value: String) { 
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(TAG, "addSelectedCaseSheetFilter: Case, Spreadsheet ID, or API Service is missing.")
            return
        }
        viewModelScope.launch {
            try {
                apiService.addSheet(currentCase.spreadsheetId, FILTERS_SHEET_NAME) 
                if (apiService.appendData(currentCase.spreadsheetId, FILTERS_SHEET_NAME, listOf(listOf(name, value))) != null) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId) 
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

    fun addAllegationToSelectedCase(allegationText: String) { 
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || allegationText.isBlank() || apiService == null) {
            Log.w(TAG, "addAllegationToSelectedCase: Missing data.")
            return
        }
        viewModelScope.launch {
            try {
                if (apiService.addAllegationToCase(currentCase.spreadsheetId, allegationText)) {
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id) 
                } else {
                    Log.w(TAG, "Failed to add allegation to case ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding allegation to case ${currentCase.id}", e)
            }
        }
    }

    private suspend fun loadSelectedCaseEvidence(spreadsheetId: String, caseIdForAssociation: Int) { 
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

    fun addEvidenceToSelectedCase(entry: Evidence) { 
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(TAG, "addEvidenceToSelectedCase: Missing data.")
            return
        }
        val entryWithCaseId = entry.copy(caseId = currentCase.id) 
        viewModelScope.launch {
            try {
                if (apiService.addEvidenceToCase(currentCase.spreadsheetId, entryWithCaseId)) {
                    loadSelectedCaseEvidence(currentCase.spreadsheetId, currentCase.id)
                } else {
                    showError("Failed to add evidence.")
                    Log.w(TAG, "Failed to add evidence to case ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding evidence to case ${currentCase.id}", e)
            }
        }
    }
    
    fun addEvidenceToUiList(uri: Uri, context: Context) { 
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true) {
                imageUriForReview = uri
                _imageBitmapForReview.value = loadBitmapFromUri(uri, context)
                if (_imageBitmapForReview.value == null) {
                    Log.e(TAG, "Failed to load bitmap for review from URI: $uri")
                    imageUriForReview = null 
                }
            } else {
                val evidence = when (mimeType) {
                    "text/plain" -> parseTextFile(uri, context)
                    "application/pdf" -> parsePdfFile(uri, context)
                    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                    else -> { Log.w(TAG, "Unsupported file type: $mimeType for URI: $uri"); null }
                }
                evidence?.let {
                    _uiEvidenceList.value = _uiEvidenceList.value + it
                }
            }
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: $uri", e)
            null
        }
    }

    fun rotateImageBeingReviewed(degrees: Float) {
        val currentBitmap = _imageBitmapForReview.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
            _imageBitmapForReview.value = rotatedBitmap
        }
    }

    private fun preprocessImageForOcr(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Apply adaptive thresholding
        val binaryMat = Mat()
        Imgproc.adaptiveThreshold(
            grayMat,
            binaryMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )

        // Convert back to bitmap
        val resultBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(binaryMat, resultBitmap)

        return resultBitmap
    }

    fun confirmImageReview(context: Context) {
        val reviewedBitmap = _imageBitmapForReview.value
        val reviewedUri = imageUriForReview

        if (reviewedBitmap == null || reviewedUri == null) {
            Log.w(TAG, "confirmImageReview: Bitmap or URI for review is null.")
            cancelImageReview()
            return
        }

        viewModelScope.launch {
            _isOcrInProgress.value = true
            try {
                val preprocessedBitmap = preprocessImageForOcr(reviewedBitmap)
                val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                val visionText = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { continuation ->
                    recognizer.process(inputImage)
                        .addOnSuccessListener { text -> 
                            if (continuation.isActive) continuation.resume(text) 
                        }
                        .addOnFailureListener { e ->
                            showError("Failed to recognize text.")
                            Log.e(TAG, "ML Kit text recognition failed during review confirmation", e)
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                } ?: return@launch

                val newEvidence = Evidence(
                    content = visionText.text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = reviewedUri.toString(),
                    documentDate = System.currentTimeMillis()
                )
                _uiEvidenceList.value = _uiEvidenceList.value + newEvidence
                Log.d(TAG, "Image review confirmed. Evidence added to UI list.")

            } catch (e: Exception) {
                Log.e(TAG, "Exception during image review confirmation or text recognition.", e)
            } finally {
                _isOcrInProgress.value = false
                cancelImageReview()
            }
        }
    }

    fun cancelImageReview() {
        _imageBitmapForReview.value = null
        imageUriForReview = null
        Log.d(TAG, "Image review cancelled/cleared.")
    }


    private suspend fun parseTextFile(uri: Uri, context: Context): Evidence? { 
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse text file", e); null }
    }

    private suspend fun parsePdfFile(uri: Uri, context: Context): Evidence? { 
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

    private suspend fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? { 
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val text = buildString { /* ... */ } 
                workbook.close()
                Evidence(content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), documentDate = System.currentTimeMillis())
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to parse spreadsheet file", e); null }
    }

    private suspend fun parseDocFile(uri: Uri, context: Context): Evidence? { 
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
    
    fun exportToSheet() { 
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

    fun processUiEvidenceForReview() { 
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.map { evidence ->
                val parserResult = scriptRunner.runScript(script, evidence)
                TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content) 
            }
            EvidenceRepository.setTaggedEvidence(taggedList)
        }
    }
    
    fun addSettingScreenFilter(name: String, value: String) { 
        val newFilter = SheetFilter(name = name, value = value)
        _settingScreenFilters.value = _settingScreenFilters.value + newFilter
        Log.d(TAG, "Added setting screen filter: $newFilter. Current setting filters: ${_settingScreenFilters.value}")
    }

    fun addTextEvidenceToSelectedCase(text: String, context: Context) { 
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
                 _uiEvidenceList.value = _uiEvidenceList.value + Evidence( 
                    content = text, timestamp = System.currentTimeMillis(), sourceDocument = "Text Input (No Case)", documentDate = System.currentTimeMillis()
                )
            }
        }
    }
    
    fun importSpreadsheet(spreadsheetIdToImport: String) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch Unit.also { Log.w(TAG, "importSpreadsheet: No API service.") }
            val sheetsData = apiService.readSpreadsheet(spreadsheetIdToImport)
            if (sheetsData != null) {
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

    fun storeTaggedDataToSheet(newTaggedData: Map<String, List<String>>) { 
        Log.d(TAG, "storeTaggedDataToSheet called with: $newTaggedData")
        _taggedData.value = newTaggedData 

        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase != null && apiService != null) {
            viewModelScope.launch {
                val spreadsheetId = currentCase.spreadsheetId
                Log.d(TAG, "Storing tagged data to spreadsheet: $spreadsheetId")
                newTaggedData.forEach { (tag, data) ->
                    if (data.isNotEmpty()) {
                        Log.d(TAG, "Adding sheet '$tag' with data: $data")
                        apiService.addSheet(spreadsheetId, tag) 
                        val values = data.map { listOf(it) }
                        apiService.appendData(spreadsheetId, tag, values)
                    }
                }
            }
        } else {
            Log.w(TAG, "storeTaggedDataToSheet: No case or API service, skipping sheet update.")
        }
    }

    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context) { 
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
        private const val FILTERS_SHEET_NAME = "Filters" 
    }
}
