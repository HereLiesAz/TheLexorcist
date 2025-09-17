package com.hereliesaz.lexorcist.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PhotoGroupViewModel @Inject constructor() : ViewModel() {
    private val _photoUris = MutableStateFlow<List<Uri>>(emptyList())
    val photoUris = _photoUris.asStateFlow()

    private val _latestTmpUri = MutableStateFlow<Uri?>(null)
    val latestTmpUri = _latestTmpUri.asStateFlow()

    fun addPhoto(uri: Uri) {
        _photoUris.value = _photoUris.value + uri
    }

    fun addPhotos(uris: List<Uri>) {
        _photoUris.value = _photoUris.value + uris
    }

    fun removePhoto(uri: Uri) {
        _photoUris.value = _photoUris.value - uri
    }

    fun setLatestTmpUri(uri: Uri) {
        _latestTmpUri.value = uri
    }
}
