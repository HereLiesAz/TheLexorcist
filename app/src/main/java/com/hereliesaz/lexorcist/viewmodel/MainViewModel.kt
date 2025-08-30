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
import com.hereliesaz.lexorcist.DataParser
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
import com.hereliesaz.lexorcist.data.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: com.hereliesaz.lexorcist.data.EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val googleApiService: GoogleApiService
) : AndroidViewModel(application) {

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

    // --- Case Management (Moved to CaseViewModel) ---

    // --- HTML Templates ---
    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    // --- Data for Selected Case (Moved to CaseViewModel and EvidenceViewModel) ---

    // --- Generic Evidence List (Moved to EvidenceViewModel) ---

    // --- Filters for SettingsScreen (in-memory) ---
    private val _settingScreenFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val settingScreenFilters: StateFlow<List<SheetFilter>> = _settingScreenFilters.asStateFlow()

    // --- Error Handling ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Uploading State ---
    private val _isUploadingFile = MutableStateFlow(false)
    val isUploadingFile: StateFlow<Boolean> = _isUploadingFile.asStateFlow()


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
        // Moved to CaseViewModel
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
        // Moved to CaseViewModel
    }

    private suspend fun loadSelectedCaseSheetFilters(spreadsheetId: String) {
        // Moved to CaseViewModel
    }
    
    fun addSelectedCaseSheetFilter(name: String, value: String) {
        // Moved to CaseViewModel
    }

    private suspend fun loadAllegationsForSelectedCase(spreadsheetId: String, caseIdForAssociation: Int) {
        // Moved to CaseViewModel
    }

    fun addAllegationToSelectedCase(allegationText: String) {
        // Moved to CaseViewModel
    }


    fun addEvidenceToSelectedCase(entry: Evidence) {
        val currentCase = _selectedCase.value
        if (currentCase == null) {
            Log.w(tag, "addEvidenceToSelectedCase: Missing data.")
            return
        }
        viewModelScope.launch {
            evidenceRepository.addEvidence(
                caseId = currentCase.id,
                content = entry.content,
                sourceDocument = entry.sourceDocument,
                category = entry.category,
                allegationId = entry.allegationId
            )
        }
        // Moved to EvidenceViewModel
    }
    
    fun addEvidenceToUiList(uri: Uri, context: Context) {
        // Moved to EvidenceViewModel
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

    fun runScriptOnEvidence(evidence: Evidence) {
        viewModelScope.launch {
            val parserResult = scriptRunner.runScript(script, evidence)
            val updatedEvidence = evidence.copy(tags = parserResult.tags)
            evidenceRepository.updateEvidence(updatedEvidence)
        }
    }

    fun processUiEvidenceForReview() {
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.mapNotNull { evidence ->
                when (val result = scriptRunner.runScript(script, evidence)) {
                    is com.hereliesaz.lexorcist.util.Result.Success -> {
                        val parserResult = result.data
                        TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content)
                    }
                    is com.hereliesaz.lexorcist.util.Result.Error -> {
                        _errorMessage.value = "Script execution failed: ${result.exception.message}"
                        null
                    }
                }
            }
            if (taggedList.size == _uiEvidenceList.value.size) {
                TaggedEvidenceRepository.setTaggedEvidence(taggedList)
            }
        }
    }
    
    fun addSettingScreenFilter(name: String, value: String) { 
        val newFilter = SheetFilter(name = name, value = value)
        _settingScreenFilters.value = _settingScreenFilters.value + newFilter
        Log.d(tag, "Added setting screen filter: $newFilter. Current setting filters: ${_settingScreenFilters.value}")
    }

    fun addTextEvidenceToSelectedCase(text: String, context: Context) {
        // Moved to EvidenceViewModel
    }
    
    fun importSpreadsheet(spreadsheetIdToImport: String) {
        // Moved to CaseViewModel
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
        // Moved to EvidenceViewModel
    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploadingFile.value = true
            try {
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
                            val file = File(context.cacheDir, fileName)
                            file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                            when (val result = apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")) {
                                is com.hereliesaz.lexorcist.util.Result.Success -> {
                                    val uploadedDriveFile = result.data
                                    Log.d(tag, "File $fileName uploaded to Drive for case ${currentCase.name}")
                                    val newEvidenceEntry = Evidence(
                                        id = 0,
                                        caseId = currentCase.id,
                                    content = "Uploaded file: $fileName (Content not extracted for preview)",
                                    timestamp = System.currentTimeMillis(),
                                    sourceDocument = uploadedDriveFile.name ?: fileName,
                                    documentDate = System.currentTimeMillis(),
                                    allegationId = null,
                                    category = mimeType ?: "file",
                                    tags = listOf("drive_upload")
                                )
                                addEvidenceToSelectedCase(newEvidenceEntry)
                                }
                                is com.hereliesaz.lexorcist.util.Result.Error -> {
                                    _errorMessage.value = "Failed to upload file: ${result.exception.message}"
                                }
                            }
                        }
                    }
                } else {
                    Log.w(tag, "addDriveFileEvidenceToSelectedCase: No case or API service.")
                }
            } finally {
                _isUploadingFile.value = false
            }
        }
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
                        val file = File(context.cacheDir, fileName)
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "audio/mpeg")
                    }
                }
            }
        }
    }

    companion object {
        private const val FILTERS_SHEET_NAME = "Filters" 
    }
}
