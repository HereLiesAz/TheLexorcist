package com.hereliesaz.lexorcist.model

object TranscriptionModels {
    private const val VOSK_BASE_URL = "https://alphacephei.com/vosk/models/"
    // Using a placeholder for Whisper models. In a real scenario, these would be hosted.
    private const val WHISPER_BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

    val voskModels by lazy {
        listOf(
            LanguageModel("English (US)", "en-us", "${VOSK_BASE_URL}vosk-model-small-en-us-0.15.zip", "vosk-model-small-en-us-0.15"),
            LanguageModel("English (Indian)", "en-in", "${VOSK_BASE_URL}vosk-model-small-en-in-0.4.zip", "vosk-model-small-en-in-0.4"),
            LanguageModel("Chinese", "cn", "${VOSK_BASE_URL}vosk-model-small-cn-0.22.zip", "vosk-model-small-cn-0.22"),
            LanguageModel("Russian", "ru", "${VOSK_BASE_URL}vosk-model-small-ru-0.22.zip", "vosk-model-small-ru-0.22"),
            LanguageModel("French", "fr", "${VOSK_BASE_URL}vosk-model-small-fr-0.22.zip", "vosk-model-small-fr-0.22"),
            LanguageModel("German", "de", "${VOSK_BASE_URL}vosk-model-small-de-0.15.zip", "vosk-model-small-de-0.15"),
            LanguageModel("Spanish", "es", "${VOSK_BASE_URL}vosk-model-small-es-0.42.zip", "vosk-model-small-es-0.42"),
            LanguageModel("Portuguese", "pt", "${VOSK_BASE_URL}vosk-model-small-pt-0.3.zip", "vosk-model-small-pt-0.3"),
            LanguageModel("Italian", "it", "${VOSK_BASE_URL}vosk-model-small-it-0.22.zip", "vosk-model-small-it-0.22"),
            LanguageModel("Japanese", "ja", "${VOSK_BASE_URL}vosk-model-small-ja-0.22.zip", "vosk-model-small-ja-0.22"),
            LanguageModel("Korean", "ko", "${VOSK_BASE_URL}vosk-model-small-ko-0.22.zip", "vosk-model-small-ko-0.22"),
            LanguageModel("Vietnamese", "vn", "${VOSK_BASE_URL}vosk-model-small-vn-0.4.zip", "vosk-model-small-vn-0.4"),
            LanguageModel("Catalan", "ca", "${VOSK_BASE_URL}vosk-model-small-ca-0.4.zip", "vosk-model-small-ca-0.4"),
            LanguageModel("Farsi", "fa", "${VOSK_BASE_URL}vosk-model-small-fa-0.42.zip", "vosk-model-small-fa-0.42"),
            LanguageModel("Ukrainian", "uk", "${VOSK_BASE_URL}vosk-model-small-uk-v3-nano.zip", "vosk-model-small-uk-v3-nano"),
            LanguageModel("Hindi", "hi", "${VOSK_BASE_URL}vosk-model-small-hi-0.22.zip", "vosk-model-small-hi-0.22"),
            LanguageModel("Czech", "cs", "${VOSK_BASE_URL}vosk-model-small-cs-0.4-rhasspy.zip", "vosk-model-small-cs-0.4-rhasspy"),
            LanguageModel("Polish", "pl", "${VOSK_BASE_URL}vosk-model-small-pl-0.22.zip", "vosk-model-small-pl-0.22"),
            LanguageModel("Turkish", "tr", "${VOSK_BASE_URL}vosk-model-small-tr-0.3.zip", "vosk-model-small-tr-0.3")
        )
    }

    // Models from https://github.com/ggerganov/whisper.cpp/tree/master/models
    // Using the tiny models for mobile.
    val whisperModels by lazy {
        listOf(
            LanguageModel("English", "en", "${WHISPER_BASE_URL}ggml-tiny.en.bin", "ggml-tiny.en"),
            LanguageModel("Multilingual", "multi", "${WHISPER_BASE_URL}ggml-tiny.bin", "ggml-tiny")
            // The whisper.cpp models are generally multilingual or english-only.
            // The library can select the language internally from the multilingual model.
            // For simplicity, we'll offer the tiny english and tiny multilingual models.
        )
    }
}
