package com.hereliesaz.lexorcist.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds a photo group while it is being assembled.
 *
 * Backed by [SavedStateHandle] rather than plain fields. Taking a photo starts
 * the system camera, which puts this app in the background; Android is free to
 * kill the process while it is there, and does so routinely under memory
 * pressure and always when "Don't keep activities" is on. A plain `ViewModel`
 * does not survive that.
 *
 * The consequence was not a visible failure. On return, the launcher's callback
 * ran with `success = true` against a freshly constructed view model whose
 * `latestTmpUri` was null, so `addPhoto` was never called and the photo was
 * dropped without a message -- along with every photo already added to the
 * group. `SavedStateHandle` is restored from the saved instance state, so both
 * the pending capture and the accumulated group come back.
 */
@HiltViewModel
class PhotoGroupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _photoUris = MutableStateFlow(savedStateHandle.get<List<String>>(KEY_PHOTOS).orEmpty().map(Uri::parse))
    val photoUris = _photoUris.asStateFlow()

    private val _latestTmpUri = MutableStateFlow(savedStateHandle.get<String>(KEY_PENDING)?.let(Uri::parse))
    val latestTmpUri = _latestTmpUri.asStateFlow()

    /** The description typed so far, kept for the same reason the photos are. */
    private val _description = MutableStateFlow(savedStateHandle.get<String>(KEY_DESCRIPTION).orEmpty())
    val description = _description.asStateFlow()

    fun addPhoto(uri: Uri) = setPhotos(_photoUris.value + uri)

    fun addPhotos(uris: List<Uri>) = setPhotos(_photoUris.value + uris)

    fun removePhoto(uri: Uri) = setPhotos(_photoUris.value - uri)

    fun setLatestTmpUri(uri: Uri?) {
        _latestTmpUri.value = uri
        savedStateHandle[KEY_PENDING] = uri?.toString()
    }

    fun setDescription(value: String) {
        _description.value = value
        savedStateHandle[KEY_DESCRIPTION] = value
    }

    /** Clears the group once it has been saved as evidence. */
    fun clear() {
        setPhotos(emptyList())
        setLatestTmpUri(null)
        setDescription("")
    }

    /**
     * Records a capture that completed.
     *
     * Returns false when the pending URI is missing, which can only happen if
     * the saved state was lost too. The caller reports that rather than
     * silently discarding the photo, as the previous `?.let { }` did.
     */
    fun onCaptureSucceeded(): Boolean {
        val pending = _latestTmpUri.value ?: return false
        addPhoto(pending)
        setLatestTmpUri(null)
        return true
    }

    private fun setPhotos(uris: List<Uri>) {
        _photoUris.value = uris
        savedStateHandle[KEY_PHOTOS] = ArrayList(uris.map(Uri::toString))
    }

    private companion object {
        const val KEY_PHOTOS = "photo_group_uris"
        const val KEY_PENDING = "photo_group_pending_capture"
        const val KEY_DESCRIPTION = "photo_group_description"
    }
}
