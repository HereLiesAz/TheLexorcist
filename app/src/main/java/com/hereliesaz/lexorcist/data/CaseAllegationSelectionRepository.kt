package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.StateFlow

/**
 * A repository that manages the set of selected allegations for the currently active case.
 *
 * This repository is stateful and holds the current selection in memory, following the same
 * pattern as the `CaseRepository` for the selected case.
 */
interface CaseAllegationSelectionRepository {

    /**
     * A [StateFlow] that emits the current set of selected allegations for the active case.
     * The set will be empty if no case is selected or if no allegations have been selected.
     */
    val selectedAllegations: StateFlow<Set<Allegation>>

    /**
     * Adds an allegation to the current selection for the active case.
     *
     * If the allegation is already selected, this method has no effect.
     * This operation will also persist the change to the underlying storage.
     *
     * @param allegation The allegation to add.
     */
    suspend fun addAllegation(allegation: Allegation)

    /**
     * Removes an allegation from the current selection for the active case.
     *
     * If the allegation is not in the current selection, this method has no effect.
     * This operation will also persist the change to the underlying storage.
     *
     * @param allegation The allegation to remove.
     */
    suspend fun removeAllegation(allegation: Allegation)

    /**
     * Clears all selected allegations. This is typically called when the selected case is cleared.
     */
    suspend fun clear()
}