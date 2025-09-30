package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.ErrorReporter
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseAllegationSelectionRepositoryImpl @Inject constructor(
    private val caseRepository: CaseRepository,
    private val storageService: StorageService,
    private val errorReporter: ErrorReporter
) : CaseAllegationSelectionRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _selectedAllegations = MutableStateFlow<Set<Allegation>>(emptySet())
    override val selectedAllegations: StateFlow<Set<Allegation>> = _selectedAllegations.asStateFlow()

    private var currentSpreadsheetId: String? = null

    init {
        caseRepository.selectedCase
            .onEach { case ->
                if (case?.spreadsheetId != currentSpreadsheetId) {
                    currentSpreadsheetId = case?.spreadsheetId
                    loadSelectedAllegations()
                }
            }
            .launchIn(repositoryScope)
    }

    private fun loadSelectedAllegations() {
        val spreadsheetId = currentSpreadsheetId
        if (spreadsheetId == null) {
            _selectedAllegations.value = emptySet()
            return
        }

        repositoryScope.launch {
            when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
                is Result.Success -> {
                    _selectedAllegations.value = result.data.toSet()
                }
                is Result.Error -> {
                    errorReporter.reportError(result.exception)
                    _selectedAllegations.value = emptySet()
                }
                else -> {
                    // Handle other states if necessary
                }
            }
        }
    }

    override suspend fun addAllegation(allegation: Allegation) {
        val spreadsheetId = currentSpreadsheetId ?: return
        if (_selectedAllegations.value.any { it.id == allegation.id }) return

        _selectedAllegations.value = _selectedAllegations.value + allegation
        when (val result = storageService.addAllegation(spreadsheetId, allegation)) {
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                // Rollback on failure
                _selectedAllegations.value = _selectedAllegations.value - allegation
            }
            else -> {
                // Success or other states
            }
        }
    }

    override suspend fun removeAllegation(allegation: Allegation) {
        val spreadsheetId = currentSpreadsheetId ?: return
        if (_selectedAllegations.value.none { it.id == allegation.id }) return

        _selectedAllegations.value = _selectedAllegations.value - allegation
        when (val result = storageService.removeAllegation(spreadsheetId, allegation)) {
            is Result.Error -> {
                errorReporter.reportError(result.exception)
                // Rollback on failure
                _selectedAllegations.value = _selectedAllegations.value + allegation
            }
            else -> {
                // Success or other states
            }
        }
    }

    override suspend fun clear() {
        _selectedAllegations.value = emptySet()
    }
}