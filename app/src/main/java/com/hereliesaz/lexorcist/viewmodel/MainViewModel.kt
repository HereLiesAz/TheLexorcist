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
// import android.provider.Settings.Global.putString // Removed as unused
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
import com.google.gson.Gson
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.SpreadsheetParser
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
// import org.opencv.android.Utils // Removed as unused
// import org.opencv.core.Mat // Removed as unused
// import org.opencv.imgproc.Imgproc // Removed as unused
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence // Corrected import
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.data.TaggedEvidenceRepository
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
// import androidx.room.util.copy // Removed as unused
import com.hereliesaz.lexorcist.data.Case

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application, private val evidenceRepository: com.hereliesaz.lexorcist.data.EvidenceRepository) : AndroidViewModel(application) {

    private val tag = "MainViewModelCombined" // Changed TAG to tag

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
    // Corrected Evidence type
    private val _selectedCaseEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val selectedCaseEvidenceList: StateFlow<List<Evidence>> = _selectedCaseEvidenceList.asStateFlow()

    // --- Generic Evidence List (for UI parsing, MainScreen display, generic export) ---
    // Corrected Evidence type
    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    // --- Filters for SettingsScreen (in-memory) ---
    private val _settingScreenFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val settingScreenFilters: StateFlow<List<SheetFilter>> = _settingScreenFilters.asStateFlow()

    // --- Error Handling ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


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
            _selectedCase.collect { case ->
                if (case != null) {
                    // Corrected: Convert Int to Long for getEvidenceForCase
                    evidenceRepository.getEvidenceForCase(case.id.toLong()).collect {
                        _selectedCaseEvidenceList.value = it
                    }
                } else {
                    _selectedCaseEvidenceList.value = emptyList()
                }
            }
        }
        viewModelScope.launch {
            _googleApiService.collect { apiService ->
                if (apiService != null && _isSignedIn.value) {
                    loadCasesFromRegistry()
                    loadHtmlTemplates() // Load HTML templates
                    _selectedCase.value?.let { currentCase ->
                        if (currentCase.spreadsheetId.isNotBlank()) {
                            loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                            loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                            evidenceRepository.refreshEvidenceForCase(currentCase.spreadsheetId, currentCase.id)
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


    // --- Sign-In Logic ---
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
        (evidenceRepository as EvidenceRepositoryImpl).setGoogleApiService(apiService)
        _googleApiService.value = apiService
        _isSignedIn.value = true
        GoogleApiServiceHolder.googleApiService = apiService
        Log.d(tag, "onSignInSuccess: Signed in.")
        viewModelScope.launch {
            loadCasesFromRegistry()
            loadHtmlTemplates() // Load HTML templates
            _selectedCase.value?.let { currentCase ->
                if (currentCase.spreadsheetId.isNotBlank()) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId)
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id)
                    evidenceRepository.refreshEvidenceForCase(currentCase.spreadsheetId, currentCase.id)
                }
            }
        }
    }

    private fun onSignInFailed() {
        (evidenceRepository as EvidenceRepositoryImpl).setGoogleApiService(null)
        _googleApiService.value = null
        _isSignedIn.value = false
        Log.d(tag, "onSignInFailed: Sign in failed.")
        _cases.value = emptyList()
        _htmlTemplates.value = emptyList() // Clear templates
        _selectedCase.value = null
        _selectedCaseSheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _selectedCaseEvidenceList.value = emptyList()
    }

    fun onSignOut() {
        (evidenceRepository as EvidenceRepositoryImpl).setGoogleApiService(null)
        _googleApiService.value = null
        _isSignedIn.value = false
        GoogleApiServiceHolder.googleApiService = null
        Log.d(tag, "onSignOut: Signed out.")
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
        sharedPref.edit {
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
        sharedPref.edit {
            putBoolean("is_dark_mode", isDark)
        }
    }

    // --- Case Management & Google Drive/Sheets ---
    private suspend fun loadCasesFromRegistry() {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(tag, "loadCasesFromRegistry: No API service.") }
        try {
            val appRootFolderId = apiService.getOrCreateAppRootFolder() ?: return Unit.also { Log.e(tag, "Failed to get app root folder.") }
            val registryId = apiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId) ?: return Unit.also { Log.e(tag, "Failed to get case registry.") }
            _cases.value = apiService.getAllCasesFromRegistry(registryId)
            Log.d(tag, "Loaded ${_cases.value.size} cases.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading cases", e)
            _cases.value = emptyList()
        }
    }

    private suspend fun loadHtmlTemplates() {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(tag, "loadHtmlTemplates: No API service.") }
        try {
            _htmlTemplates.value = apiService.listHtmlTemplatesInAppRootFolder()
            Log.d(tag, "Loaded ${_htmlTemplates.value.size} HTML templates.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading HTML templates", e)
            _htmlTemplates.value = emptyList()
        }
    }

    private suspend fun generatePdfFromHtmlString(htmlString: String, outputPdfName: String, context: Context): File? = suspendCancellableCoroutine { continuation ->
        Log.d(tag, "generatePdfFromHtmlString for $outputPdfName. HTML length: ${htmlString.length}")

        // val outputPdfFile = File(context.cacheDir, outputPdfName) // Commented out as unused for now
        var webView: WebView? = null

        viewModelScope.launch(Dispatchers.Main) { // WebView operations must be on the Main thread
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

                        // val jobName = "${context.packageName} Document" // Commented out as unused
                        // val printAttributes = PrintAttributes.Builder() // Commented out as unused
                        //     .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        //     .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        //     .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        //     .build()

                        // Fallback: resume with null since the direct PDF generation is currently commented out
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
        if (case != null && case.spreadsheetId.isNotBlank() && _googleApiService.value != null) {
            viewModelScope.launch {
                loadSelectedCaseSheetFilters(case.spreadsheetId)
                loadAllegationsForSelectedCase(case.spreadsheetId, case.id)
                evidenceRepository.refreshEvidenceForCase(case.spreadsheetId, case.id)
            }
        } else {
            _selectedCaseSheetFilters.value = emptyList()
            _allegations.value = emptyList()
        }
    }

    private suspend fun loadSelectedCaseSheetFilters(spreadsheetId: String) { 
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(tag, "loadSelectedCaseSheetFilters: No API service.") }
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
            _selectedCaseSheetFilters.value = emptyList()
        }
    }
    
    fun addSelectedCaseSheetFilter(name: String, value: String) { 
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || apiService == null) {
            Log.w(tag, "addSelectedCaseSheetFilter: Case, Spreadsheet ID, or API Service is missing.")
            return
        }
        viewModelScope.launch {
            try {
                apiService.addSheet(currentCase.spreadsheetId, FILTERS_SHEET_NAME) 
                if (apiService.appendData(currentCase.spreadsheetId, FILTERS_SHEET_NAME, listOf(listOf(name, value))) != null) {
                    loadSelectedCaseSheetFilters(currentCase.spreadsheetId) 
                } else {
                    Log.w(tag, "Failed to add sheet filter '$name' to sheet.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding sheet filter '$name'", e)
            }
        }
    }

    private suspend fun loadAllegationsForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        val apiService = _googleApiService.value ?: return Unit.also { Log.w(tag, "loadAllegations: No API service.") }
         if (spreadsheetId.isBlank()) {
            _allegations.value = emptyList(); return
        }
        try {
            _allegations.value = apiService.getAllegationsForCase(spreadsheetId, caseIdForAssociation)
            Log.d(tag, "Loaded ${_allegations.value.size} allegations for case $caseIdForAssociation.")
        } catch (e: Exception) {
            Log.e(tag, "Error loading allegations for $spreadsheetId", e)
            _allegations.value = emptyList()
        }
    }

    fun addAllegationToSelectedCase(allegationText: String) { 
        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase == null || currentCase.spreadsheetId.isBlank() || allegationText.isBlank() || apiService == null) {
            Log.w(tag, "addAllegationToSelectedCase: Missing data.")
            return
        }
        viewModelScope.launch {
            try {
                if (apiService.addAllegationToCase(currentCase.spreadsheetId, allegationText)) {
                    loadAllegationsForSelectedCase(currentCase.spreadsheetId, currentCase.id) 
                } else {
                    Log.w(tag, "Failed to add allegation to case ${currentCase.id}.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding allegation to case ${currentCase.id}", e)
            }
        }
    }


    fun addEvidenceToSelectedCase(entry: Evidence) {
        val currentCase = _selectedCase.value
        if (currentCase == null) {
            Log.w(tag, "addEvidenceToSelectedCase: Missing data.")
            return
        }
        val entryWithCaseId = entry.copy(caseId = currentCase.id)
        viewModelScope.launch {
            evidenceRepository.addEvidence(currentCase.spreadsheetId, entryWithCaseId)
        }
    }
    
    fun addEvidenceToUiList(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true) {
                imageUriForReview = uri
                _imageBitmapForReview.value = loadBitmapFromUri(uri, context)
                if (_imageBitmapForReview.value == null) {
                    Log.e(tag, "Failed to load bitmap for review from URI: $uri")
                    imageUriForReview = null
                }
            } else {
                val evidence : Evidence? = when (mimeType) {
                    "text/plain" -> parseTextFile(uri, context)
                    "application/pdf" -> parsePdfFile(uri, context)
                    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                    else -> { Log.w(tag, "Unsupported file type: $mimeType for URI: $uri"); null }
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
            Log.e(tag, "Error loading bitmap from URI: $uri", e)
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
        // Placeholder for actual preprocessing
        return bitmap
    }

    fun confirmImageReview(context: Context) {
        val reviewedBitmap = _imageBitmapForReview.value
        val reviewedUri = imageUriForReview

        if (reviewedBitmap == null || reviewedUri == null) {
            Log.w(tag, "confirmImageReview: Bitmap or URI for review is null.")
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
                            _errorMessage.value = "Failed to recognize text." // Corrected showError
                            Log.e(tag, "ML Kit text recognition failed during review confirmation", e)
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                } ?: return@launch

                val newEvidence = Evidence(
                    id = 0, // Will be overridden by DB, or use _uiEvidenceList.value.size for temp UI id
                    caseId = _selectedCase.value?.id ?: 0,
                    content = visionText.text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = reviewedUri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "OCR Image", // Example category
                    tags = emptyList() // Example tags
                )
                _uiEvidenceList.value = _uiEvidenceList.value + newEvidence
                Log.d(tag, "Image review confirmed. Evidence added to UI list.")

            } catch (e: Exception) {
                Log.e(tag, "Exception during image review confirmation or text recognition.", e)
                 _errorMessage.value = "Error processing image: ${e.localizedMessage}"
            } finally {
                _isOcrInProgress.value = false
                cancelImageReview()
            }
        }
    }

    fun cancelImageReview() {
        _imageBitmapForReview.value = null
        imageUriForReview = null
        Log.d(tag, "Image review cancelled/cleared.")
    }

    // Corrected to non-suspend, ensure ID logic is appropriate for UI list
    private fun parseTextFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = _selectedCase.value?.id ?: 0, 
                    content = text, 
                    timestamp = System.currentTimeMillis(), 
                    sourceDocument = uri.toString(), 
                    documentDate = System.currentTimeMillis(), 
                    allegationId = null, 
                    category = "Text File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse text file", e); _errorMessage.value = "Failed to parse text file"; null }
    }

    private fun parsePdfFile(uri: Uri, context: Context): Evidence? { 
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val text = buildString { for (i in 1..pdfDocument.numberOfPages) { append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))) } }
                pdfDocument.close()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = _selectedCase.value?.id ?: 0, 
                    content = text, 
                    timestamp = System.currentTimeMillis(), 
                    sourceDocument = uri.toString(), 
                    documentDate = System.currentTimeMillis(), 
                    allegationId = null, 
                    category = "PDF File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse PDF file", e); _errorMessage.value = "Failed to parse PDF file"; null }
    }

    private fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val text = buildString { /* For now, content will be empty. Implement actual parsing later */ }
                workbook.close()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = _selectedCase.value?.id ?: 0, 
                    content = text, 
                    timestamp = System.currentTimeMillis(), 
                    sourceDocument = uri.toString(), 
                    documentDate = System.currentTimeMillis(), 
                    allegationId = null, 
                    category = "Spreadsheet File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse spreadsheet file", e); _errorMessage.value = "Failed to parse spreadsheet file"; null }
    }

    private fun parseDocFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    WordExtractor(HWPFDocument(inputStream)).text
                } else {
                    XWPFWordExtractor(XWPFDocument(inputStream)).text
                }
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = _selectedCase.value?.id ?: 0, 
                    content = text, 
                    timestamp = System.currentTimeMillis(), 
                    sourceDocument = uri.toString(), 
                    documentDate = System.currentTimeMillis(), 
                    allegationId = null, 
                    category = "Document File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse document file", e); _errorMessage.value = "Failed to parse document file"; null }
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
            var currentUiId = _uiEvidenceList.value.size // For UI temporary uniqueness
            cursor?.use {
                if (it.moveToFirst()) {
                    val bodyIndex = it.getColumnIndex(android.provider.Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(android.provider.Telephony.Sms.DATE)
                    do {
                        val body = it.getString(bodyIndex)
                        val dateMillis = it.getLong(dateIndex)
                        smsList.add(
                            Evidence(
                                id = 0, // Placeholder ID
                                caseId = _selectedCase.value?.id ?: 0, 
                                content = body, 
                                timestamp = dateMillis, 
                                sourceDocument = "SMS Message", 
                                documentDate = dateMillis, 
                                allegationId = null, 
                                category = "SMS",
                                tags = emptyList()
                            )
                        )
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
                    val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString()) } // Example data
                    apiService.appendData(id, "Sheet1", data)
                }
            }
        }
    }

    fun setScript(scriptText: String) { this.script = scriptText }

    fun processUiEvidenceForReview() {
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.map { evidence ->
                val parserResult = scriptRunner.runScript(script, evidence) // Changed evidence.content to evidence
                TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content) // Changed evidence.id to evidence and evidence to evidence.content
            }
            TaggedEvidenceRepository.setTaggedEvidence(taggedList)
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
            val apiService = _googleApiService.value
            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch
                val timestamp = System.currentTimeMillis()
                val file = File(context.cacheDir, "text-evidence-$timestamp.txt")
                file.writeText(text)
                val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")
                if (uploadedDriveFile != null) {
                    val newEvidence = Evidence(
                        id = 0, // DB will assign ID
                        caseId = currentCase.id, 
                        content = text, 
                        timestamp = timestamp,
                        sourceDocument = uploadedDriveFile.name ?: "Uploaded Text Evidence", 
                        documentDate = timestamp,
                        allegationId = null, 
                        category = "Text Upload",
                        tags = emptyList()
                    )
                    addEvidenceToSelectedCase(newEvidence)
                } else {
                    _errorMessage.value = "Failed to upload text evidence."
                }
            } else {
                Log.w(tag, "addTextEvidenceToSelectedCase: No case or API service. Adding to UI list only.")
                 val newEvidenceForUiList = Evidence(
                    id = 0, // Temp UI id or manage better if needed for UI keys
                    caseId = 0,
                    content = text, 
                    timestamp = System.currentTimeMillis(), 
                    sourceDocument = "Text Input (No Case)", 
                    documentDate = System.currentTimeMillis(), 
                    allegationId = null, 
                    category = "Local Text",
                    tags = emptyList()
                )
                _uiEvidenceList.value = _uiEvidenceList.value + newEvidenceForUiList
            }
        }
    }
    
    fun importSpreadsheet(spreadsheetIdToImport: String) {
        viewModelScope.launch {
            val apiService = _googleApiService.value ?: return@launch Unit.also { Log.w(tag, "importSpreadsheet: No API service.") }
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
                        loadCasesFromRegistry()
                    } else {
                        Log.w(tag, "Failed to parse/store spreadsheet: $spreadsheetIdToImport")
                        _errorMessage.value = "Failed to parse or store spreadsheet."
                    }
                } catch (e: Exception) {
                     Log.e(tag, "Error during spreadsheet import (schema or parsing): $spreadsheetIdToImport", e)
                    _errorMessage.value = "Error importing spreadsheet: ${e.localizedMessage}"
                }
            } else {
                Log.w(tag, "Failed to read spreadsheet data for ID: $spreadsheetIdToImport")
                _errorMessage.value = "Failed to read spreadsheet data."
            }
        }
    }

    fun storeTaggedDataToSheet(newTaggedData: Map<String, List<String>>) { 
        Log.d(tag, "storeTaggedDataToSheet called with: $newTaggedData")
        _taggedData.value = newTaggedData 

        val currentCase = _selectedCase.value
        val apiService = _googleApiService.value
        if (currentCase != null && apiService != null) {
            viewModelScope.launch {
                val spreadsheetId = currentCase.spreadsheetId
                Log.d(tag, "Storing tagged data to spreadsheet: $spreadsheetId")
                newTaggedData.forEach { (tag, data) ->
                    if (data.isNotEmpty()) {
                        Log.d(tag, "Adding sheet '$tag' with data: $data")
                        apiService.addSheet(spreadsheetId, tag) 
                        val values = data.map { listOf(it) }
                        apiService.appendData(spreadsheetId, tag, values)
                    }
                }
            }
        } else {
            Log.w(tag, "storeTaggedDataToSheet: No case or API service, skipping sheet update.")
        }
    }

    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context) { 
        viewModelScope.launch {
            val currentCase = _selectedLiesAz.value
            val apiService = _googleApiService.value
            if (currentCase != null && apiService != null) {
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(currentCase.name) ?: return@launch
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    val mimeType = context.contentResolver.getType(uri)
                    context.contentResolver.openInputStream(uri)?.let { inputStream ->
                        val file = File(context.cacheDir, fileName)
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                        Log.d(tag, "File $fileName uploaded to Drive for case ${currentCase.name}")
                        if (uploadedDriveFile != null) {
                             val newEvidenceEntry = Evidence(
                                 id = 0, // DB will assign ID
                                 caseId = currentCase.id,
                                 content = "Uploaded file: $fileName (Content not extracted for preview)",
                                 timestamp = System.currentTimeMillis(),
                                 sourceDocument = uploadedDriveFile.name ?: fileName,
                                 documentDate = System.currentTimeMillis(),
                                 allegationId = null,
                                 category = mimeType ?: "file",
                                 tags = listOf("drive_upload")
                             )
                            // Consider if this should be added to the _selectedCaseEvidenceList or _uiEvidenceList immediately
                            // or if addEvidenceToSelectedCase (which persists to sheet) should be called.
                            // addEvidenceToSelectedCase(newEvidenceEntry)
                        } else {
                            _errorMessage.value = "Failed to upload file to Drive."
                        }
                    }
                }
            } else {
                Log.w(tag, "addDriveFileEvidenceToSelectedCase: No case or API service.")
            }
        }
    }

    fun updateExtractedText(text: String) {
        _extractedText.value = text
    }

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
    }
    
    companion object {
        private const val FILTERS_SHEET_NAME = "Filters" 
    }
}
