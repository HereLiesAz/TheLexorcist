package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.data.ActiveScriptRepository
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.LocalFileStorageService
import com.hereliesaz.lexorcist.data.Script
import com.hereliesaz.lexorcist.data.ScriptRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.model.CleanupSuggestion
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.DisplayExhibit
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.utils.DataParser
import com.hereliesaz.lexorcist.model.OutlookSignInState
import com.hereliesaz.lexorcist.service.GmailService
import com.hereliesaz.lexorcist.service.ImapService
import com.hereliesaz.lexorcist.data.JurisdictionRepository
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.repository.ExhibitRepository
import com.hereliesaz.lexorcist.data.repository.LegalRepository
import com.hereliesaz.lexorcist.service.OutlookService
import com.hereliesaz.lexorcist.utils.ChatHistoryParser
import com.hereliesaz.lexorcist.utils.EvidenceImporter
import com.hereliesaz.lexorcist.utils.LocationHistoryParser
import com.hereliesaz.lexorcist.model.UiComponentModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.hereliesaz.lexorcist.service.CleanupService
import com.hereliesaz.lexorcist.service.OcrProcessingService
import com.hereliesaz.lexorcist.service.PackagingService
import com.hereliesaz.lexorcist.service.VideoProcessingService
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Locale

enum class AllegationSortType {
    TYPE,
    CATEGORY,
    NAME,
    COURT_LEVEL,
}

@HiltViewModel
class CaseViewModel
@Inject
constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val caseRepository: CaseRepository,
    private val evidenceRepository: EvidenceRepository,
    private val localFileStorageService: LocalFileStorageService,
    val scriptRunner: com.hereliesaz.lexorcist.service.ScriptRunner,
    private val ocrProcessingService: OcrProcessingService,
    private val videoProcessingService: VideoProcessingService,
    private val workManager: WorkManager,
    private val scriptRepository: ScriptRepository,
    private val activeScriptRepository: ActiveScriptRepository,
    private val logService: com.hereliesaz.lexorcist.service.LogService,
    private val storageService: com.hereliesaz.lexorcist.data.StorageService,
    private val globalLoadingState: com.hereliesaz.lexorcist.service.GlobalLoadingState,
    private val googleApiService: com.hereliesaz.lexorcist.service.GoogleApiService,
    private val settingsManager: SettingsManager,
    private val evidenceImporter: EvidenceImporter,
    private val chatHistoryParser: ChatHistoryParser,
    private val gmailService: GmailService,
    private val outlookService: OutlookService,
    private val imapService: ImapService,
    private val outlookAuthManager: com.hereliesaz.lexorcist.auth.OutlookAuthManager,
    private val jurisdictionRepository: JurisdictionRepository,
    private val locationHistoryParser: LocationHistoryParser,
    private val exhibitRepository: ExhibitRepository,
    private val legalRepository: LegalRepository,
    private val cleanupService: CleanupService,
    private val packagingService: PackagingService
) : ViewModel() {
    private val sharedPref =
        applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    val isLoading: StateFlow<Boolean> = globalLoadingState.isLoading

    private val _processingStatus = MutableStateFlow<String?>(null) // Consider removing if _processingState covers this
    val processingStatus: StateFlow<String?> = _processingStatus.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val cases: StateFlow<List<Case>> =
        caseRepository.cases
            .map { cases -> cases.filter { !it.isArchived } }
            .combine(sortOrder) { cases, currentSortOrder ->
                when (currentSortOrder) {
                    SortOrder.NAME_ASC -> cases.sortedBy { it.name }
                    SortOrder.NAME_DESC -> cases.sortedByDescending { it.name }
                    SortOrder.DATE_ASC -> cases.sortedBy { it.id }
                    SortOrder.DATE_DESC -> cases.sortedByDescending { it.id }
                }
            }
            .combine(searchQuery) { cases, query ->
                if (query.isBlank()) {
                    cases
                } else {
                    cases.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _vmSelectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _vmSelectedCase.asStateFlow()

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    // Allegations Section
    private val _allegationSearchQuery = MutableStateFlow("")
    val allegationSearchQuery: StateFlow<String> = _allegationSearchQuery.asStateFlow()

    private val _allegationSortType = MutableStateFlow(AllegationSortType.TYPE)
    val allegationSortType: StateFlow<AllegationSortType> = _allegationSortType.asStateFlow()

    private val _masterAllegations = MutableStateFlow<List<MasterAllegation>>(emptyList())

    private val selectedCaseAllegations = caseRepository.selectedCaseAllegations

    val selectedAllegations: StateFlow<List<MasterAllegation>> =
        combine(_masterAllegations, selectedCaseAllegations) { master, selected ->
            val selectedIds = selected.map { it.id.toString() }.toSet()
            master.filter { selectedIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val allegations: StateFlow<List<MasterAllegation>> =
        combine(
            _masterAllegations,
            selectedCaseAllegations,
            allegationSearchQuery,
            allegationSortType
        ) { master, selected, query, sort ->
            val selectedIds = selected.map { it.id.toString() }.toSet()
            val updatedMaster = master.map {
                it.copy(isSelected = selectedIds.contains(it.id))
            }

            val filtered = if (query.isBlank()) {
                updatedMaster
            } else {
                updatedMaster.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            (it.description?.contains(query, ignoreCase = true) ?: false) ||
                            (it.category?.contains(query, ignoreCase = true) ?: false) ||
                            (it.type?.contains(query, ignoreCase = true) ?: false)
                }
            }

            when (sort) {
                AllegationSortType.TYPE ->
                    filtered.sortedWith(compareBy({ it.type ?: "" }, { it.category ?: "" }, { it.name }))
                AllegationSortType.CATEGORY ->
                    filtered.sortedWith(compareBy({ it.category ?: "" }, { it.type ?: "" }, { it.name }))
                AllegationSortType.NAME -> filtered.sortedBy { it.name }
                AllegationSortType.COURT_LEVEL -> filtered.sortedBy { it.courtLevel ?: "" }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exhibits = MutableStateFlow<List<com.hereliesaz.lexorcist.data.Exhibit>>(emptyList())
    val exhibits: StateFlow<List<com.hereliesaz.lexorcist.data.Exhibit>> = _exhibits.asStateFlow()

    private val _selectedExhibit = MutableStateFlow<DisplayExhibit?>(null)
    val selectedExhibit: StateFlow<DisplayExhibit?> = _selectedExhibit.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val displayExhibits: StateFlow<List<DisplayExhibit>> =
        selectedCase.flatMapLatest { case ->
            if (case == null) {
                flowOf(emptyList<DisplayExhibit>())
            } else {
                // Explicitly typed local flow variables
                val allegationsFlow: kotlinx.coroutines.flow.Flow<List<Allegation>> = caseRepository.selectedCaseAllegations
                val catalogFlow: kotlinx.coroutines.flow.Flow<List<com.hereliesaz.lexorcist.data.ExhibitCatalogItem>> = exhibitRepository.getExhibitCatalog()
                val caseExhibitsFlow: kotlinx.coroutines.flow.Flow<List<com.hereliesaz.lexorcist.data.Exhibit>> = evidenceRepository.getExhibitsForCase(case.spreadsheetId)

                // Combine with explicit type arguments
                combine<List<Allegation>, List<com.hereliesaz.lexorcist.data.ExhibitCatalogItem>, List<com.hereliesaz.lexorcist.data.Exhibit>, List<DisplayExhibit>>(
                    allegationsFlow,
                    catalogFlow,
                    caseExhibitsFlow
                ) { currentAllegations, catalogItems, caseExhibitsList -> // Lambda params without explicit types
                    val selectedAllegationIds = currentAllegations.mapNotNull { it.id }.toSet()

                    if (selectedAllegationIds.isEmpty()) {
                        emptyList<DisplayExhibit>()
                    } else {
                        catalogItems.filter { catalogItem ->
                            catalogItem.applicableAllegationIds.any { it in selectedAllegationIds }
                        }.map { pertinentCatalogItem ->
                            val matchingCaseExhibit =
                                caseExhibitsList.find { it.name == pertinentCatalogItem.type }
                            DisplayExhibit(
                                catalogItem = pertinentCatalogItem,
                                caseExhibit = matchingCaseExhibit
                            )
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _pertinentExhibitTypes = MutableStateFlow<List<String>>(emptyList())
    val pertinentExhibitTypes: StateFlow<List<String>> = _pertinentExhibitTypes.asStateFlow()

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: StateFlow<List<Script>> = _scripts.asStateFlow()

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates.asStateFlow()

    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _userRecoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val userRecoverableAuthIntent: StateFlow<Intent?> =
        _userRecoverableAuthIntent.asStateFlow()

    private val _videoProcessingProgress = MutableStateFlow<String?>(null)
    val videoProcessingProgress: StateFlow<String?> = _videoProcessingProgress.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState?>(null) // Used for general media processing UI
    val processingState: StateFlow<ProcessingState?> = _processingState.asStateFlow()

    private val _logMessages = MutableStateFlow<List<com.hereliesaz.lexorcist.model.LogEntry>>(emptyList())
    val logMessages: StateFlow<List<com.hereliesaz.lexorcist.model.LogEntry>> = _logMessages.asStateFlow()

    private val _navigateToTranscriptionScreen = MutableSharedFlow<Int>()
    val navigateToTranscriptionScreen = _navigateToTranscriptionScreen.asSharedFlow()

    private val _timelineSortType = MutableStateFlow(TimelineSortType.DATE_OF_OCCURRENCE)
    val timelineSortType: StateFlow<TimelineSortType> = _timelineSortType.asStateFlow()

    private val _jurisdictions = MutableStateFlow<List<com.hereliesaz.lexorcist.model.Court>>(emptyList())
    val jurisdictions: StateFlow<List<com.hereliesaz.lexorcist.model.Court>> = _jurisdictions.asStateFlow()

    private val _selectedCaseEvidenceListInternal =
        MutableStateFlow<List<com.hereliesaz.lexorcist.data.Evidence>>(emptyList())

    val selectedCaseEvidenceList: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        _selectedCaseEvidenceListInternal
            .combine(timelineSortType) { evidence, sortType ->
                when (sortType) {
                    TimelineSortType.DATE_OF_OCCURRENCE -> evidence.sortedByDescending { it.documentDate }
                    TimelineSortType.DATE_EVIDENCE_ADDED -> evidence.sortedByDescending { it.timestamp }
                    TimelineSortType.BY_ALLEGATION -> evidence.sortedBy { it.allegationId }
                    TimelineSortType.BY_FILE_TYPE -> evidence.sortedBy { it.type }
                    TimelineSortType.CUSTOM -> evidence
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedEvidence: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        selectedCaseEvidenceList
            .map { list -> list.filter { it.isSelected } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unassignedEvidence: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        combine(selectedCaseEvidenceList, exhibits) { evidenceList, exhibitsList ->
            val assignedIds = exhibitsList.flatMap { it.evidenceIds }.toSet()
            evidenceList.filter { it.id !in assignedIds }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _cleanupSuggestions = MutableStateFlow<List<CleanupSuggestion>>(emptyList())
    val cleanupSuggestions: StateFlow<List<CleanupSuggestion>> = _cleanupSuggestions.asStateFlow()

    private val _isScanningForCleanup = MutableStateFlow(false)
    val isScanningForCleanup: StateFlow<Boolean> = _isScanningForCleanup.asStateFlow()

    private val _dynamicUiComponents = MutableStateFlow<List<UiComponentModel>>(emptyList())
    val dynamicUiComponents: StateFlow<List<UiComponentModel>> = _dynamicUiComponents.asStateFlow()

    fun addOrUpdateDynamicUiComponent(component: UiComponentModel) {
        val currentComponents = _dynamicUiComponents.value.toMutableList()
        val index = currentComponents.indexOfFirst { it.id == component.id }
        if (index != -1) {
            currentComponents[index] = component
        } else {
            currentComponents.add(component)
        }
        _dynamicUiComponents.value = currentComponents
    }

    fun onTimelineSortOrderChanged(sortType: TimelineSortType) {
        _timelineSortType.value = sortType
    }

    private val _themeMode =
        MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> =
        _themeMode.asStateFlow()

    private val _storageLocation = MutableStateFlow<String?>(null)
    val storageLocation: StateFlow<String?> = _storageLocation.asStateFlow()

    init {
        Log.d("CaseViewModel", "--- CaseViewModel INIT --- instancia: $this")
        loadThemeModePreference()
        loadJurisdictions()
        loadMasterAllegations()
        // Set default storage location on init
        viewModelScope.launch {
            val savedLocation = settingsManager.getStorageLocation()?.first()
            if (savedLocation?.toString().isNullOrEmpty()) {
                _storageLocation.value = applicationContext.filesDir.absolutePath
                Log.i("CaseViewModel", "Storage location initialized to default: ${_storageLocation.value}")
            } else {
                _storageLocation.value = savedLocation.toString()
                Log.i("CaseViewModel", "Storage location loaded from settings: $savedLocation")
            }
        }


        viewModelScope.launch {
            logService.logEventFlow.collect { newLog ->
                _logMessages.value = listOf(newLog) + _logMessages.value.take(199) // Keep last 200 logs
            }
        }

        viewModelScope.launch {
            Log.d("CaseViewModel", "INIT: Starting to collect caseRepository.selectedCase. Current repo instance: ${caseRepository}")
            caseRepository.selectedCase.collect { caseFromRepo ->
                Log.d("CaseViewModel", "INIT COLLECTOR: caseRepository.selectedCase emitted: ${caseFromRepo?.name ?: "null"}. Updating _vmSelectedCase.")
                _vmSelectedCase.value = caseFromRepo
            }
        }

        viewModelScope.launch {
            caseRepository.selectedCaseEvidence.collect { result ->
                when (result) {
                    is Result.Success -> {
                        _selectedCaseEvidenceListInternal.value = result.data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.message ?: "Error loading evidence"
                        _selectedCaseEvidenceListInternal.value = emptyList()
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                        _selectedCaseEvidenceListInternal.value = emptyList()
                    }
                    is Result.Loading -> {
                    }
                }
            }
        }

        viewModelScope.launch {
            val lastSelectedCaseId = sharedPref.getString("last_selected_case_id", null)
            if (lastSelectedCaseId != null) {
                val allCases = caseRepository.cases.first()
                val lastSelectedCase = allCases.find { it.spreadsheetId == lastSelectedCaseId }
                if (lastSelectedCase != null) {
                    selectCase(lastSelectedCase)
                }
            }
        }
    }

    fun packageFilesForCase(files: List<File>, packageName: String, extension: String) {
        viewModelScope.launch {
            val destinationUri = android.net.Uri.parse("${storageLocation.value}/$packageName.$extension")
            packagingService.createArchive(files, destinationUri)
        }
    }

    fun selectExhibit(displayExhibit: DisplayExhibit?) {
        _selectedExhibit.value = displayExhibit
    }

    fun removeEvidenceFromExhibit(exhibitId: Int, evidenceId: Int) {
        viewModelScope.launch {
            val exhibit = _exhibits.value.find { it.id == exhibitId }
            if (exhibit != null) {
                val updatedEvidenceIds = exhibit.evidenceIds.toMutableList().apply {
                    remove(evidenceId)
                }
                val updatedExhibit = exhibit.copy(evidenceIds = updatedEvidenceIds)
                selectedCase.value?.let {
                    evidenceRepository.updateExhibit(it.spreadsheetId, updatedExhibit)
                }
                loadExhibits() // Refresh the list
            }
        }
    }

    fun importLocationHistoryFromFile(uri: android.net.Uri, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val locationRecords = locationHistoryParser.parse(uri, applicationContext.contentResolver)
                val filteredRecords = locationRecords.filter { it.timestampMillis in startDate..endDate }

                if (filteredRecords.isNotEmpty()) {
                    filteredRecords.forEach { record ->
                        val locationContent = "Latitude: ${record.latitude}\nLongitude: ${record.longitude}"
                        val evidence = Evidence(
                            caseId = selectedCase.value?.id?.toLong() ?: 0,
                            spreadsheetId = selectedCase.value?.spreadsheetId ?: "",
                            type = "Location",
                            content = locationContent,
                            formattedContent = locationContent,
                            mediaUri = null,
                            timestamp = record.timestampMillis,
                            sourceDocument = "Location History Import",
                            documentDate = record.timestampMillis,
                            allegationId = null,
                            allegationElementName = null,
                            category = "Location",
                            tags = listOf("location", "history")
                        )
                        evidenceRepository.addEvidence(evidence)
                    }
                    _userMessage.value = "Imported ${filteredRecords.size} location records."
                } else {
                    _userMessage.value = "No location records found in the selected date range."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to import location history: ${e.message}"
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun assignEvidenceToDynamicExhibit(evidenceId: Int, exhibitType: String) {
        viewModelScope.launch {
            val case = selectedCase.value ?: return@launch
            val existingExhibit = _exhibits.value.find { it.name.equals(exhibitType, ignoreCase = true) }

            if (existingExhibit != null) {
                // Exhibit exists, add evidence to it
                val updatedEvidenceIds = existingExhibit.evidenceIds.toMutableList().apply {
                    if (!contains(evidenceId)) {
                        add(evidenceId)
                    }
                }
                val updatedExhibit = existingExhibit.copy(evidenceIds = updatedEvidenceIds)
                evidenceRepository.updateExhibit(case.spreadsheetId, updatedExhibit)
            } else {
                // Exhibit does not exist, create a new one
                val newExhibit = com.hereliesaz.lexorcist.data.Exhibit(
                    caseId = case.id.toLong(),
                    name = exhibitType,
                    description = "Evidence related to $exhibitType",
                    evidenceIds = listOf(evidenceId)
                )
                evidenceRepository.addExhibit(case.spreadsheetId, newExhibit)
            }
            // Refresh the exhibits list to reflect the change
            loadExhibits()
        }
    }


    fun updateExhibit(exhibit: com.hereliesaz.lexorcist.data.Exhibit) {
        viewModelScope.launch {
            selectedCase.value?.let {
                evidenceRepository.updateExhibit(it.spreadsheetId, exhibit)
                loadExhibits()
            }
        }
    }

    fun deleteExhibit(exhibit: com.hereliesaz.lexorcist.data.Exhibit) {
        viewModelScope.launch {
            selectedCase.value?.let {
                evidenceRepository.deleteExhibit(it.spreadsheetId, exhibit)
                loadExhibits()
            }
        }
    }

    fun setStorageLocation(uri: android.net.Uri) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                settingsManager.saveStorageLocation(uri.toString())
                _storageLocation.value = uri.toString()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    private fun clearCaseData() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.selectCase(null)
                _sheetFilters.value = emptyList()
                _htmlTemplates.value = emptyList()
                _plaintiffs.value = ""
                _defendants.value = ""
                _court.value = ""
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearUserRecoverableAuthIntent() {
        _userRecoverableAuthIntent.value = null
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        sharedPref.edit().putString("theme_mode", themeMode.name).apply()
    }

    private fun loadThemeModePreference() {
        val themeName =
            sharedPref.getString(
                "theme_mode",
                ThemeMode.SYSTEM.name
            )
        _themeMode.value =
            ThemeMode.valueOf(
                themeName ?: ThemeMode.SYSTEM.name
            )
    }

    fun onSortOrderChange(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAllegationSearchQueryChanged(query: String) {
        _allegationSearchQuery.value = query
    }

    fun onAllegationSortTypeChanged(sortType: AllegationSortType) {
        _allegationSortType.value = sortType
    }

    private fun loadMasterAllegations() {
        viewModelScope.launch {
            legalRepository.getMasterAllegations().collect {
                _masterAllegations.value = it
            }
        }
    }

    fun toggleAllegationSelection(allegation: MasterAllegation) {
        viewModelScope.launch {
            val case = selectedCase.value
                ?: run {
                    _errorMessage.value = "Cannot toggle allegation: No case selected."
                    return@launch
                }

            val allegationId = allegation.id?.toIntOrNull()
            if (allegationId == null) {
                _errorMessage.value = "Cannot toggle allegation: Invalid ID."
                return@launch
            }

            val repoAllegation = Allegation(
                id = allegationId,
                spreadsheetId = case.spreadsheetId,
                name = allegation.name
            )

            val isCurrentlySelected = selectedCaseAllegations.first().any { it.id == allegationId }

            if (isCurrentlySelected) {
                caseRepository.removeAllegation(repoAllegation)
            } else {
                caseRepository.addAllegation(repoAllegation)
            }
        }
    }

    fun loadCasesFromRepository() {
        Log.d("CaseViewModel", "loadCasesFromRepository called.")
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            _processingState.value = ProcessingState.InProgress(0f)
            _userMessage.value = "Loading cases..."
            try {
                caseRepository.refreshCases() // This fetches from the remote source
                caseRepository.cases.collect { updatedCases -> // This collects the updated local flow
                    // The collected list is already assigned to the `cases` StateFlow
                    // by the stateIn operator at the top level.
                    // This block can be used for any additional logic needed after collection.
                }
                _userMessage.value = "Cases loaded successfully."
                _processingState.value = ProcessingState.Completed("Cases loaded successfully.")
            } catch (e: Exception) {
                Log.e("CaseViewModel", "Error loading cases: ${e.message}", e)
                _errorMessage.value = "Error loading cases: ${e.message}"
                _processingState.value = ProcessingState.Failure("Error loading cases: ${e.message}")
            } finally {
                globalLoadingState.popLoading()
                if (_processingState.value is ProcessingState.InProgress) {
                    _processingState.value = ProcessingState.Idle
                }
            }
        }
    }

    fun loadHtmlTemplatesFromRepository() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            _processingState.value = ProcessingState.InProgress(0f)
            _userMessage.value = "Loading HTML templates..."
            try {
                caseRepository.refreshHtmlTemplates()
                caseRepository.getHtmlTemplates().collect {
                    _htmlTemplates.value = it
                }
                _userMessage.value = "HTML templates loaded successfully."
                _processingState.value = ProcessingState.Completed("HTML templates loaded successfully.")
            } catch (e: Exception) {
                val errorMsg = "Error loading HTML templates: ${e.message}"
                Log.e("CaseViewModel", "Error loading HTML templates: $errorMsg", e)
                _errorMessage.value = errorMsg
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                globalLoadingState.popLoading()
                if (_processingState.value is ProcessingState.InProgress) {
                    _processingState.value = ProcessingState.Idle
                }
            }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            _processingState.value = ProcessingState.InProgress(0f)
            _userMessage.value = "Importing spreadsheet..."
            try {
                caseRepository.importSpreadsheet(spreadsheetId)
                _userMessage.value = "Spreadsheet imported successfully."
                _processingState.value = ProcessingState.Completed("Spreadsheet imported successfully.")
            } catch (e: Exception) {
                val errorMsg = "Error importing spreadsheet: ${e.message}"
                Log.e("CaseViewModel", errorMsg, e)
                _errorMessage.value = errorMsg
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                globalLoadingState.popLoading()
                if (_processingState.value is ProcessingState.InProgress) {
                    _processingState.value = ProcessingState.Idle
                }
            }
        }
    }

    fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
    ) {
        Log.d("CaseViewModel", "createCase called with name: $caseName")
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                Log.d("CaseViewModel", "Calling caseRepository.createCase from within createCase")
                val result =
                    caseRepository.createCase(
                        caseName,
                        exhibitSheetName,
                        caseNumber,
                        caseSection,
                        caseJudge,
                        plaintiffs.value,
                        defendants.value,
                        court.value,
                    )
                when (result) {
                    is Result.Success -> {
                        Log.d("CaseViewModel", "Case creation successful")
                    }
                    is Result.Error -> {
                        _errorMessage.value =
                            result.exception.message ?: "Unknown error during case creation"
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                    }
                    is Result.Loading -> { }
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun selectCase(case: Case?) {
        Log.d("CaseViewModel", "--- CaseViewModel.selectCase ENTERED with case: ${case?.name ?: "null"} --- instance: $this, repo instance: $caseRepository")
        viewModelScope.launch {
            Log.d("CaseViewModel", "viewModelScope.launch in selectCase for case: ${case?.name ?: "null"}")
            globalLoadingState.pushLoading()
            try {
                _processingState.value = ProcessingState.Idle
                _logMessages.value = emptyList()

                caseRepository.selectCase(case)
                if (case != null) {
                    sharedPref.edit().putString("last_selected_case_id", case.spreadsheetId).apply()
                } else {
                    sharedPref.edit().remove("last_selected_case_id").apply()
                }
                Log.d("CaseViewModel", "IMMEDIATELY AFTER caseRepository.selectCase, ViewModel's _vmSelectedCase.value is: ${_vmSelectedCase.value?.name ?: "null"}")

                if (case != null) {
                    Log.d("CaseViewModel", "Case is not null, proceeding to load filters/templates for ${case.name}")
                    loadSheetFiltersFromRepository(case.spreadsheetId)
                    loadHtmlTemplatesFromRepository()
                    // Exhibit creation is now handled by the allegations collector,
                    // which is triggered when the case's allegations are loaded after selection.
                } else {
                    Log.d("CaseViewModel", "Case is null, clearing filters/templates.")
                    _sheetFilters.value = emptyList()
                    _htmlTemplates.value = emptyList()
                }
            } finally {
                globalLoadingState.popLoading()
                Log.d("CaseViewModel", "isLoading SET TO false in selectCase finally block for case: ${case?.name ?: "null"}")
            }
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.refreshSheetFilters(spreadsheetId)
                caseRepository.getSheetFilters(spreadsheetId).collect {
                    _sheetFilters.value = it
                }
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun addSheetFilterWithRepository(
        name: String,
        value: String,
    ) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: return@launch
                caseRepository.addSheetFilter(spreadsheetId, name, value)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }


    fun toggleEvidenceSelection(evidenceId: Int) {
        val updatedList =
            _selectedCaseEvidenceListInternal.value.map {
                if (it.id == evidenceId) {
                    it.copy(isSelected = !it.isSelected)
                } else {
                    it
                }
            }
        _selectedCaseEvidenceListInternal.value = updatedList
    }

    fun clearEvidenceSelection() {
        val list =
            _selectedCaseEvidenceListInternal.value.map {
                it.copy(isSelected = false)
            }
        _selectedCaseEvidenceListInternal.value = list
    }

    fun assignAllegationToEvidence(
        evidenceId: Int,
        allegationId: String?,
    ) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val evidence = _selectedCaseEvidenceListInternal.value.find { it.id == evidenceId }
                if (evidence != null) {
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        viewModelScope.launch {
            selectedCase.value?.let {
                val updatedCase = it.copy(plaintiffs = name)
                caseRepository.updateCase(updatedCase)
            }
        }
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        viewModelScope.launch {
            selectedCase.value?.let {
                val updatedCase = it.copy(defendants = name)
                caseRepository.updateCase(updatedCase)
            }
        }
    }

    fun onCourtSelected(courtName: String) {
        _court.value = courtName
        viewModelScope.launch {
            selectedCase.value?.let {
                val updatedCase = it.copy(court = courtName)
                caseRepository.updateCase(updatedCase)
            }
        }
    }

    private fun loadJurisdictions() {
        viewModelScope.launch {
            try {
                _jurisdictions.value = jurisdictionRepository.getJurisdictions()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load jurisdictions: ${e.message}"
                Log.e("CaseViewModel", "Error loading jurisdictions", e)
            }
        }
    }

    fun updateEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.updateEvidence(evidence)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun archiveCaseWithRepository(case: Case) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.archiveCase(case)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.deleteCase(case)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.clearCache()
                clearCaseData()
                sharedPref.edit().clear().apply()
                loadThemeModePreference()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun rerunAllScriptsOnAllEvidence() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            _userMessage.value = "Rerunning all active scripts on all evidence..."
            try {
                val allEvidence = _selectedCaseEvidenceListInternal.value
                val activeScriptIds = activeScriptRepository.activeScriptIds.value
                val allScripts = scriptRepository.getScripts()
                val scriptsToRun = allScripts.filter { activeScriptIds.contains(it.id) }
                val sortedScriptsToRun = scriptsToRun.sortedBy { script -> activeScriptIds.indexOf(script.id) }

                allEvidence.forEach { evidence ->
                    var updatedEvidence = evidence
                    sortedScriptsToRun.forEach { script ->
                        val result = scriptRunner.runScript(script.content, updatedEvidence)
                        if (result is Result.Success) {
                            val currentTagsInEvidence: List<String> = updatedEvidence.tags
                            val newTagsFromScript: List<String> = result.data.tags
                            val combinedTags: List<String> = (currentTagsInEvidence + newTagsFromScript).distinct()
                            updatedEvidence = updatedEvidence.copy(tags = combinedTags)
                        }
                    }
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
                _userMessage.value = "Finished rerunning scripts."
                caseRepository.refreshSelectedCaseDetails()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rerun scripts: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun deleteEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.deleteEvidence(evidence)
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun assignAllegationToSelectedEvidence(allegationId: String?) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                selectedEvidence.value.forEach { evidence ->
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
                clearEvidenceSelection()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
        _processingState.value = ProcessingState.Idle
    }

    fun addTextEvidence(text: String, allegationElementName: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addTextEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    Log.w("CaseViewModel", "addTextEvidence: No case selected from StateFlow, aborting.")
                    viewModelScope.launch { _userMessage.emit("Please select a case first to add text evidence.") }
                    globalLoadingState.popLoading()
                    return@launch
                }
                val entities = DataParser.tagData(text)
                val newEvidence =
                    com.hereliesaz.lexorcist.data.Evidence(
                        caseId = caseToUse.id.toLong(),
                        spreadsheetId = caseToUse.spreadsheetId,
                        type = "text",
                        content = text,
                        formattedContent = null,
                        mediaUri = null,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Manual text entry",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        allegationElementName = allegationElementName,
                        category = "",
                        tags = emptyList(),
                        commentary = null,
                        entities = entities,
                    )
                evidenceRepository.addEvidence(newEvidence)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun updateCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.updateCommentary(evidenceId, commentary)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun processImageEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processImageEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            globalLoadingState.pushLoading()
            clearLogs()
            val caseToUse = currentCaseFromState ?: run {
                Log.w("CaseViewModel", "processImageEvidence: No case selected from StateFlow, aborting.")
                _userMessage.value = "Please select a case first to add image evidence."
                globalLoadingState.popLoading()
                _processingState.value = ProcessingState.Failure("No case selected")
                return@launch
            }
            try {
                Log.d("CaseViewModel", "Processing image for case: ${caseToUse.name}")
                val (newEvidence, message) = ocrProcessingService.processImage(
                    uri = uri,
                    context = applicationContext,
                    caseId = caseToUse.id.toLong(),
                    spreadsheetId = caseToUse.spreadsheetId
                ) { state -> _processingState.value = state }

                message?.let {
                    Log.i("CaseViewModel", "Message from ocrProcessingService: $it")
                    _userMessage.value = it
                }
                if (newEvidence != null && newEvidence.content.isEmpty()) {
                    Log.i("CaseViewModel", "No text found in image for URI: $uri")
                    _userMessage.value = "No text found in the image."
                }
                Log.d("CaseViewModel", "Image processing finished for URI: $uri")
            } catch (e: Exception) {
                val errorMsg = "Error processing image URI: $uri: ${e.message}"
                Log.e("CaseViewModel", errorMsg, e)
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                globalLoadingState.popLoading()
                Log.d("CaseViewModel", "processImageEvidence finally block. isLoading set to false. Final ProcessingState: ${_processingState.value}")
            }
        }
    }

    fun processAudioEvidence(uri: android.net.Uri, allegationElementName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processAudioEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            globalLoadingState.pushLoading()
            clearLogs()
            _processingState.value = ProcessingState.InProgress(0.0f)

            val caseToUse = currentCaseFromState ?: run {
                val errorMsg = "Please select a case first to add audio evidence."
                Log.w("CaseViewModel", "processAudioEvidence: No case selected. Message: $errorMsg")
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure("No case selected")
                globalLoadingState.popLoading()
                return@launch
            }

            try {
                Log.i("CaseViewModel", "Processing audio for case: ${caseToUse.name}, URI: $uri")
                _processingState.value = ProcessingState.InProgress(0.1f)

                val uploadResult = withContext(Dispatchers.IO) {
                    evidenceRepository.uploadFile(uri, caseToUse.name, caseToUse.spreadsheetId)
                }

                when (uploadResult) {
                    is Result.Success -> {
                        val uploadedFileUriString = uploadResult.data
                        Log.i("CaseViewModel", "Audio file uploaded: $uploadedFileUriString")
                        _userMessage.value = "Raw audio file saved. Starting transcription."
                        _processingState.value = ProcessingState.InProgress(0.25f)

                                _processingState.value = ProcessingState.InProgress(0.75f)

                                val fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(applicationContext, uri)
                                val newEvidence =
                                    com.hereliesaz.lexorcist.data.Evidence(
                                        caseId = caseToUse.id.toLong(),
                                        spreadsheetId = caseToUse.spreadsheetId,
                                        type = "audio",
                                        content = "",
                                        formattedContent = "```\n\n```",
                                        mediaUri = uploadedFileUriString,
                                        timestamp = System.currentTimeMillis(),
                                        sourceDocument = uploadedFileUriString,
                                        documentDate = System.currentTimeMillis(),
                                        allegationId = null,
                                        allegationElementName = allegationElementName,
                                        category = "Audio Transcription",
                                        tags = listOf("audio", "transcription"),
                                        commentary = null,
                                        parentVideoId = null,
                                        entities = emptyMap(),
                                        fileHash = fileHash
                                    )
                                val savedEvidence = withContext(Dispatchers.IO) {
                                    evidenceRepository.addEvidence(newEvidence)
                                }

                                if (savedEvidence != null) {
                                    _userMessage.value = "Audio evidence processed and saved."
                                    _processingState.value = ProcessingState.Completed("Audio processing complete.")
                                    if (savedEvidence.content.isNotEmpty()) {
                                        _navigateToTranscriptionScreen.emit(savedEvidence.id)
                                    }
                                } else {
                                    val errorMsg = "Failed to save transcribed audio evidence."
                                    Log.e("CaseViewModel", errorMsg)
                                    _userMessage.value = errorMsg
                                    _processingState.value = ProcessingState.Failure(errorMsg)
                                }
                    }
                    is Result.Error -> {
                        val errorMsg = "Error uploading audio file: ${uploadResult.exception.message}"
                        Log.e("CaseViewModel", errorMsg, uploadResult.exception)
                        _userMessage.value = errorMsg
                        _processingState.value = ProcessingState.Failure(errorMsg)
                    }
                    is Result.UserRecoverableError -> {
                        val errorMsg = "User recoverable error uploading audio: ${uploadResult.exception.message}"
                        Log.w("CaseViewModel", errorMsg, uploadResult.exception)
                        _userMessage.value = errorMsg
                        _userRecoverableAuthIntent.value = uploadResult.exception.intent
                        _processingState.value = ProcessingState.Failure(errorMsg)
                    }
                    is Result.Loading -> {
                        Log.i("CaseViewModel", "Audio upload is loading...")
                        _processingState.value = ProcessingState.InProgress(0.15f)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error processing audio URI: $uri: ${e.message}"
                Log.e("CaseViewModel", errorMsg, e)
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                globalLoadingState.popLoading()
                Log.d("CaseViewModel", "processAudioEvidence finally block. isLoading set to false. Final ProcessingState: ${_processingState.value}")
            }
        }
    }

    fun updateTranscript(evidence: com.hereliesaz.lexorcist.data.Evidence, newTranscript: String, reason: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                logService.addLog("Updating transcript for evidence ${evidence.id}")
                val result = evidenceRepository.updateTranscript(evidence, newTranscript, reason)
                if (result is Result.Error) {
                    logService.addLog("Error updating transcript: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                } else {
                    logService.addLog("Transcript updated successfully")
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun processVideoEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCase = selectedCase.value
            if (currentCase == null) {
                _userMessage.value = "Please select a case first."
                return@launch
            }
            globalLoadingState.pushLoading()
            clearLogs()
            try {
                val result =
                    videoProcessingService.processVideo(uri)

                if (result != null) {
                    _userMessage.value = "Video processed successfully."
                    _processingState.value = ProcessingState.Completed("Video processed successfully.")
                } else {
                    _userMessage.value = "Video processing failed."
                    _processingState.value = ProcessingState.Failure("Video processing failed.")
                }
            } catch (e: Exception) {
                _userMessage.value = "An error occurred during video processing: ${e.message}"
                _processingState.value = ProcessingState.Failure(e.message ?: "Unknown error")
                Log.e("CaseViewModel", "Video processing exception", e)
            } finally {
                globalLoadingState.popLoading()
                _videoProcessingProgress.value = null
            }
        }
    }

    fun addPhotoGroupEvidence(photoUris: List<android.net.Uri>, description: String, allegationElementName: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            clearLogs()
            _processingState.value = ProcessingState.InProgress(0.0f)
            var currentProgress = 0.0f
            val progressStep = if (photoUris.isNotEmpty()) 1.0f / photoUris.size else 0f

            try {
                val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addPhotoGroupEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    val errorMsg = "Please select a case first to add photos."
                    Log.w("CaseViewModel", "addPhotoGroupEvidence: No case selected. Message: $errorMsg")
                    _userMessage.value = errorMsg
                    _processingState.value = ProcessingState.Failure("No case selected for photo group")
                    globalLoadingState.popLoading()
                    return@launch
                }
                val savedPhotoPaths = mutableListOf<String>()

                photoUris.forEachIndexed { index, uri ->
                    _processingState.value = ProcessingState.InProgress(currentProgress)
                    _userMessage.value = "Uploading photo ${index + 1} of ${photoUris.size}..."
                    val mimeType = applicationContext.contentResolver.getType(uri) ?: "image/jpeg"
                    when (val result = storageService.uploadFile(caseToUse.spreadsheetId, uri, mimeType)) {
                        is Result.Success -> {
                            savedPhotoPaths.add(result.data)
                            currentProgress += progressStep
                            _processingState.value = ProcessingState.InProgress(currentProgress)
                        }
                        is Result.Error -> {
                            val errorMsg = "Failed to save photo ${index + 1}: ${result.exception.message}"
                            _userMessage.value = errorMsg
                            _processingState.value = ProcessingState.Failure(errorMsg)
                            globalLoadingState.popLoading()
                            return@launch
                        }
                        is Result.UserRecoverableError -> {
                            val errorMsg = "User error saving photo ${index + 1}: ${result.exception.message}"
                            _userMessage.value = errorMsg
                            _userRecoverableAuthIntent.value = result.exception.intent
                            _processingState.value = ProcessingState.Failure(errorMsg)
                            globalLoadingState.popLoading()
                            return@launch
                        }
                        is Result.Loading -> {
                            _userMessage.value = "Photo ${index + 1} is uploading..."
                        }
                    }
                }

                if (savedPhotoPaths.isNotEmpty() && savedPhotoPaths.size == photoUris.size) {
                    val mediaUriJson = com.google.gson.Gson().toJson(savedPhotoPaths)
                    val newEvidence =
                        com.hereliesaz.lexorcist.data.Evidence(
                            caseId = caseToUse.id.toLong(),
                            spreadsheetId = caseToUse.spreadsheetId,
                            type = "photo_group",
                            content = description,
                            formattedContent = null,
                            mediaUri = mediaUriJson,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = "Photo Group",
                            documentDate = System.currentTimeMillis(),
                            allegationId = null,
                            allegationElementName = allegationElementName,
                            category = "Photo",
                            tags = listOf("photo", "group"),
                            commentary = null,
                            entities = DataParser.tagData(description),
                        )
                    evidenceRepository.addEvidence(newEvidence)
                    _userMessage.value = "Photo group evidence saved successfully."
                    _processingState.value = ProcessingState.Completed("Photo group saved.")
                } else if (savedPhotoPaths.isEmpty() && photoUris.isNotEmpty()) {
                    if (_processingState.value !is ProcessingState.Failure) {
                        _userMessage.value = "No photos were saved."
                        _processingState.value = ProcessingState.Failure("No photos saved.")
                    }
                }

            } finally {
                globalLoadingState.popLoading()
                if (_processingState.value is ProcessingState.InProgress) {
                    _processingState.value = ProcessingState.Idle
                }
            }
        }
    }

    fun generateCleanupSuggestions() {
        viewModelScope.launch {
            _isScanningForCleanup.value = true
            globalLoadingState.pushLoading()
            _userMessage.value = "Scanning for duplicates and image series..."
            try {
                // Heavy lifting should be on a background thread
                withContext(Dispatchers.IO) {
                    val currentEvidence = _selectedCaseEvidenceListInternal.value

                    val evidenceToHash = currentEvidence.filter { it.mediaUri != null && it.fileHash.isNullOrEmpty() }
                    if (evidenceToHash.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _userMessage.value = "Calculating hashes for ${evidenceToHash.size} items..."
                        }
                        evidenceToHash.forEach { evidence ->
                            try {
                                val hash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(applicationContext, android.net.Uri.parse(evidence.mediaUri))
                                if (hash != null) {
                                    evidenceRepository.updateEvidence(evidence.copy(fileHash = hash))
                                }
                            } catch (e: SecurityException) {
                                Log.e("Cleanup", "Permission error hashing ${evidence.mediaUri}, may need to re-grant access.", e)
                                withContext(Dispatchers.Main) {
                                    _errorMessage.value = "Permission error accessing a file. Please check storage permissions."
                                }
                            } catch (e: Exception) {
                                Log.e("Cleanup", "Failed to hash ${evidence.mediaUri}", e)
                            }
                        }
                    }

                    // Refresh evidence list after hashing
                    caseRepository.refreshSelectedCaseDetails() // block until refresh is done
                    val updatedEvidence = _selectedCaseEvidenceListInternal.value
                    val suggestions = mutableListOf<CleanupSuggestion>()

                    val duplicateGroups = cleanupService.detectDuplicates(updatedEvidence)
                        .map { CleanupSuggestion.DuplicateGroup(it) }
                    suggestions.addAll(duplicateGroups)

                    val imageSeriesGroups = cleanupService.detectImageSeries(updatedEvidence)
                        .map { CleanupSuggestion.ImageSeriesGroup(it) }
                    suggestions.addAll(imageSeriesGroups)

                    withContext(Dispatchers.Main) {
                        _cleanupSuggestions.value = suggestions
                        _userMessage.value = if (suggestions.isNotEmpty()) {
                            "Cleanup scan complete. Found ${suggestions.size} suggestion(s)."
                        } else {
                            "Cleanup scan complete. No duplicates or image series found."
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to run cleanup scan: ${e.message}"
                Log.e("Cleanup", "Error in generateCleanupSuggestions", e)
            }
            finally {
                globalLoadingState.popLoading()
                _isScanningForCleanup.value = false // This ensures the loading indicator is always turned off
            }
        }
    }

    fun deleteDuplicates(group: CleanupSuggestion.DuplicateGroup) {
        viewModelScope.launch {
            val evidenceToDelete = group.evidence.drop(1)
            evidenceToDelete.forEach { evidence ->
                deleteEvidence(evidence)
            }
            generateCleanupSuggestions()
        }
    }

    fun mergeImageSeries(group: CleanupSuggestion.ImageSeriesGroup, allegationElementName: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            _userMessage.value = "Merging image series into PDF..."
            try {
                val case = selectedCase.value
                val storagePath = storageLocation.value
                if (case == null || storagePath == null) {
                    _errorMessage.value = "Cannot merge series: Case or storage location is not set."
                    return@launch
                }

                val newEvidence = withContext(Dispatchers.IO) {
                    val caseDir = File(storagePath, case.spreadsheetId)
                    val rawDir = File(caseDir, "raw").apply { if (!exists()) mkdirs() }
                    val pdfFile = File(rawDir, "merged_series_${System.currentTimeMillis()}.pdf")

                    val pdfDocument = PdfDocument()
                    try {
                        group.evidence.forEach { evidence ->
                            applicationContext.contentResolver.openInputStream(android.net.Uri.parse(evidence.mediaUri))?.use { inputStream ->
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                pdfDocument.finishPage(page)
                                bitmap.recycle()
                            }
                        }
                        pdfDocument.writeTo(FileOutputStream(pdfFile))
                    } catch (e: Exception) {
                        Log.e("MergeSeries", "Error creating PDF", e)
                        // Return null to indicate failure
                        return@withContext null
                    } finally {
                        pdfDocument.close()
                    }

                    val combinedContent = group.evidence.joinToString("\n\n") { it.content }
                    com.hereliesaz.lexorcist.data.Evidence(
                        caseId = case.id.toLong(),
                        spreadsheetId = case.spreadsheetId,
                        type = "pdf",
                        content = combinedContent,
                        formattedContent = "```\n$combinedContent\n```",
                        mediaUri = pdfFile.toUri().toString(),
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Merged from image series",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        allegationElementName = allegationElementName,
                        category = "Document",
                        tags = listOf("pdf", "merged"),
                        fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(applicationContext, pdfFile.toUri())
                    )
                }

                if (newEvidence != null) {
                    evidenceRepository.addEvidence(newEvidence)
                    group.evidence.forEach { evidence ->
                        deleteEvidence(evidence)
                    }
                    _userMessage.value = "Successfully merged images into a new PDF."
                } else {
                    _errorMessage.value = "Failed to create merged PDF."
                }

            } catch (e: Exception) {
                _errorMessage.value = "An error occurred while merging images: ${e.message}"
                Log.e("MergeSeries", "Merge failed", e)
            } finally {
                globalLoadingState.popLoading()
                // Refresh suggestions after the operation
                generateCleanupSuggestions()
            }
        }
    }

    fun loadExhibits() {
        viewModelScope.launch {
            selectedCase.value?.let {
                evidenceRepository.getExhibitsForCase(it.spreadsheetId).collect { exhibits ->
                    _exhibits.value = exhibits
                }
            }
        }
    }

    fun addExhibit(name: String, description: String) {
        viewModelScope.launch {
            selectedCase.value?.let {
                val newExhibit = com.hereliesaz.lexorcist.data.Exhibit(
                    caseId = it.id.toLong(),
                    name = name,
                    description = description,
                    evidenceIds = emptyList()
                )
                evidenceRepository.addExhibit(it.spreadsheetId, newExhibit)
                loadExhibits()
            }
        }
    }

    fun addEvidenceToExhibit(exhibitId: Int, evidenceIds: List<Int>) {
        viewModelScope.launch {
            val exhibit = _exhibits.value.find { it.id == exhibitId }
            if (exhibit != null) {
                val newEvidenceIds = evidenceIds.filter { it !in exhibit.evidenceIds }
                val updatedExhibit = exhibit.copy(evidenceIds = exhibit.evidenceIds + newEvidenceIds)
                selectedCase.value?.let {
                    evidenceRepository.updateExhibit(it.spreadsheetId, updatedExhibit)
                }
                loadExhibits()
            }
        }
    }

    fun generateDocument(exhibit: com.hereliesaz.lexorcist.data.Exhibit, template: com.google.api.services.drive.model.File) {
        viewModelScope.launch {
            val currentCase = _vmSelectedCase.value // Directly access the mutable state flow's value
            if (currentCase == null) {
                // Consider adding an error message to the user
                return@launch
            }

            val scriptId = currentCase.scriptId ?: return@launch
            val caseId = currentCase.id ?: return@launch
            val templateId = template.id ?: return@launch

            val scriptToRun = """
                lex.google.runAppsScript(
                    '$scriptId',
                    'generateDocument',
                    ['$caseId', '${exhibit.id}', '$templateId']
                );
            """.trimIndent()

            when (val result = scriptRunner.runGenericScript(scriptToRun, emptyMap())) {
                is Result.Success -> {
                    logService.addLog("Document generated successfully: ${result.data}")
                }
                is Result.Error -> {
                    logService.addLog("Error generating document: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                }
                is Result.UserRecoverableError -> {
                    logService.addLog("User recoverable error generating document: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    _userRecoverableAuthIntent.value = result.exception.intent
                }
                else -> {
                    logService.addLog("Unknown error generating document", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                }
            }
        }
    }

    fun importSmsEvidence(contact: String?, startDate: Long?, endDate: Long?) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }

                val smsEvidence = evidenceImporter.importSms(contact, startDate, endDate).map {
                    it.copy(caseId = caseId, spreadsheetId = spreadsheetId)
                }

                if (smsEvidence.isNotEmpty()) {
                    evidenceRepository.addEvidenceList(smsEvidence)
                }
                _userMessage.value = "Imported ${smsEvidence.size} SMS messages."
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import SMS messages: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importCallLogEvidence(contact: String?, startDate: Long?, endDate: Long?) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }

                val callLogEvidence = evidenceImporter.importCallLog(contact, startDate, endDate).map {
                    it.copy(caseId = caseId, spreadsheetId = spreadsheetId)
                }

                if (callLogEvidence.isNotEmpty()) {
                    evidenceRepository.addEvidenceList(callLogEvidence)
                }
                _userMessage.value = "Imported ${callLogEvidence.size} call log entries."
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import call logs: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importLocationHistoryEvidence() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val locationEvidence = evidenceImporter.importLocationHistory()
                if (locationEvidence != null) {
                    evidenceRepository.addEvidence(locationEvidence.copy(caseId = selectedCase.value?.id?.toLong() ?: 0, spreadsheetId = selectedCase.value?.spreadsheetId ?: ""))
                    _userMessage.value = "Imported last known location."
                }
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import location: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importChatHistory(uri: android.net.Uri) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }

                val chatEvidence = chatHistoryParser.parse(uri).map {
                    it.copy(caseId = caseId, spreadsheetId = spreadsheetId)
                }

                if (chatEvidence.isNotEmpty()) {
                    evidenceRepository.addEvidenceList(chatEvidence)
                }
                _userMessage.value = "Imported ${chatEvidence.size} chat messages."
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import chat history: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importEmails(from: String, subject: String, before: String, after: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }

                val messages = gmailService.searchEmails(from, subject, before, after)
                val emailEvidence = messages.map<com.google.api.services.gmail.model.Message, Evidence> { message ->
                    val subjectHeader = message.payload.headers.find { it.name == "Subject" }?.value ?: "No Subject"
                    val fromHeader = message.payload.headers.find { it.name == "From" }?.value ?: "Unknown Sender"
                    Evidence(
                        content = "From: $fromHeader\nSubject: $subjectHeader\n\n${message.snippet}",
                        type = "Email",
                        timestamp = message.internalDate,
                        caseId = caseId,
                        spreadsheetId = spreadsheetId,
                        formattedContent = null,
                        mediaUri = null,
                        sourceDocument = "Imported Email",
                        documentDate = message.internalDate,
                        allegationId = null,
                        allegationElementName = null,
                        category = "Email",
                        tags = listOf("email")
                    )
                }
                if (emailEvidence.isNotEmpty()) {
                    evidenceRepository.addEvidenceList(emailEvidence)
                }
                _userMessage.value = "Imported ${emailEvidence.size} emails."
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import emails: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importOutlookEmails(from: String, subject: String, before: String, after: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }

                val outlookState = outlookAuthManager.outlookSignInState.value
                if (outlookState is OutlookSignInState.Success) {
                    val messages = outlookService.searchEmails(outlookState.accessToken, from, subject, before, after)
                    val emailEvidence = messages?.map<com.microsoft.graph.models.Message, Evidence> { message ->
                        val receivedDateTime = message.receivedDateTime?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
                        Evidence(
                            content = "From: ${message.from?.emailAddress?.address}\nSubject: ${message.subject}\n\n${message.bodyPreview}",
                            type = "Email",
                            timestamp = receivedDateTime,
                            caseId = caseId,
                            spreadsheetId = spreadsheetId,
                            formattedContent = null,
                            mediaUri = null,
                            sourceDocument = "Imported Email",
                            documentDate = receivedDateTime,
                            allegationId = null,
                            allegationElementName = null,
                            category = "Email",
                            tags = listOf("email")
                        )
                    }
                    if (emailEvidence?.isNotEmpty() == true) {
                        evidenceRepository.addEvidenceList(emailEvidence)
                    }
                    _userMessage.value = "Imported ${emailEvidence?.size ?: 0} emails from Outlook."
                } else {
                    _userMessage.value = "Not signed in to Outlook."
                }
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import emails from Outlook: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importImapEmails(host: String, user: String, pass: String, from: String, subject: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val caseId = selectedCase.value?.id?.toLong() ?: 0
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: ""
                if (spreadsheetId.isEmpty()) {
                    _errorMessage.value = "Please select a case first."
                    return@launch
                }
                val emailEvidence = imapService.fetchEmails(host, user, pass, from, subject).map {
                    it.copy(caseId = caseId, spreadsheetId = spreadsheetId)
                }
                if (emailEvidence.isNotEmpty()) {
                    evidenceRepository.addEvidenceList(emailEvidence)
                }
                _userMessage.value = "Imported ${emailEvidence.size} emails."
            }
            catch (e: Exception) {
                _errorMessage.value = "Failed to import emails via IMAP: ${e.message}"
            }
            finally {
                globalLoadingState.popLoading()
            }
        }
    }
}