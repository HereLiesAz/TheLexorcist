package com.hereliesaz.lexorcist.model

import kotlinx.coroutines.flow.MutableStateFlow

data class LanguageModel(
    val name: String, // e.g., "English (US)"
    val code: String, // e.g., "en-us" for vosk, "en" for whisper
    val modelUrl: String,
    val modelName: String, // e.g., "vosk-model-small-en-us-0.15" or "whisper-tiny-en"
    val vocabUrl: String? = null, // For whisper
    val downloadState: MutableStateFlow<DownloadState> = MutableStateFlow(DownloadState.NotDownloaded),
    val progress: MutableStateFlow<Float> = MutableStateFlow(0f)
)

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    object Downloading : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}
