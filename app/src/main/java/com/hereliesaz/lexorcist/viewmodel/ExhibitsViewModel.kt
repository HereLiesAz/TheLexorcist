package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.CaseAllegationSelectionRepository
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import com.hereliesaz.lexorcist.data.MasterAllegationRepository
import com.hereliesaz.lexorcist.data.repository.ExhibitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExhibitsViewModel @Inject constructor(
    exhibitRepository: ExhibitRepository,
    caseRepository: CaseRepository,
    caseAllegationSelectionRepository: CaseAllegationSelectionRepository,
    masterAllegationRepository: MasterAllegationRepository
) : ViewModel() {

    private val selectedAllegationNames: StateFlow<List<String>> =
        caseRepository.selectedCase.flatMapLatest { case ->
            if (case != null) {
                caseAllegationSelectionRepository.getSelectedAllegations(case.spreadsheetId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pertinentExhibits: StateFlow<List<ExhibitCatalogItem>> = combine(
        exhibitRepository.getExhibitCatalog(),
        selectedAllegationNames,
        masterAllegationRepository.getMasterAllegations()
    ) { catalog, selectedNames, masterAllegations ->
        if (selectedNames.isEmpty()) {
            emptyList()
        } else {
            val allegationNameToIdMap = masterAllegations.associateBy({ it.name }, { it.id })
            val selectedAllegationIds = selectedNames.mapNotNull { allegationNameToIdMap[it] }.toSet()

            catalog.filter { exhibit ->
                exhibit.applicableAllegationIds.any { it in selectedAllegationIds }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}