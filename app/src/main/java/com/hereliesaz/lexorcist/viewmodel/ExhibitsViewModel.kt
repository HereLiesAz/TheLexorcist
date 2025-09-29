package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.CaseAllegationSelectionRepository
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import com.hereliesaz.lexorcist.data.SelectedAllegation
import com.hereliesaz.lexorcist.data.repository.ExhibitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExhibitsViewModel @Inject constructor(
    exhibitRepository: ExhibitRepository,
    caseRepository: CaseRepository,
    caseAllegationSelectionRepository: CaseAllegationSelectionRepository
) : ViewModel() {

    private val selectedAllegations: StateFlow<List<SelectedAllegation>> =
        caseRepository.selectedCase.flatMapLatest { case ->
            if (case != null) {
                caseAllegationSelectionRepository.getSelectedAllegations(case.spreadsheetId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pertinentExhibits: StateFlow<List<ExhibitCatalogItem>> = combine(
        exhibitRepository.getExhibitCatalog(),
        selectedAllegations
    ) { catalog, selected ->
        if (selected.isEmpty()) {
            emptyList()
        } else {
            val selectedAllegationIds = selected.map { it.id }.toSet()
            catalog.filter { exhibit ->
                exhibit.applicableAllegationIds.any { it in selectedAllegationIds }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}