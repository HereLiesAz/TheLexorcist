package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.SettingsManager // Assuming this is correctly injected if used
import com.hereliesaz.lexorcist.service.ScriptRunner // Assuming this is correctly injected if used
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    application: Application,
    private val settingsManager: SettingsManager, // Ensure these are properly provided by Hilt
    private val scriptRunner: ScriptRunner         // Ensure these are properly provided by Hilt
) : AndroidViewModel(application) {

    private val tag = "OcrViewModel"

    private val _isOcrInProgress = MutableStateFlow(false)
    val isOcrInProgress = _isOcrInProgress.asStateFlow()

    private val _imageBitmapForReview = MutableStateFlow<Bitmap?>(null)
    val imageBitmapForReview = _imageBitmapForReview.asStateFlow()

    private var imageUriForReview: Uri? = null

    private val _extractedText = MutableStateFlow("")
    val extractedText = _extractedText.asStateFlow()

    private val _newlyCreatedEvidence = MutableSharedFlow<Evidence>()
    val newlyCreatedEvidence = _newlyCreatedEvidence.asSharedFlow()

    fun startImageReview(uri: Uri, context: Context) {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(uri, context)
            if (bitmap != null) {
                imageUriForReview = uri
                _imageBitmapForReview.value = bitmap
            } else {
                Log.e(tag, "Failed to load bitmap from URI for review: $uri")
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception loading bitmap from URI: $uri", e)
            null
        }
    }

    fun rotateImageBeingReviewed(degrees: Float) {
        val currentBitmap = _imageBitmapForReview.value ?: return
        viewModelScope.launch {
            val matrix = Matrix().apply { postRotate(degrees) }
            _imageBitmapForReview.value = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
        }
    }

    fun confirmImageReview(context: Context) {
        val reviewedBitmap = _imageBitmapForReview.value ?: return
        val currentImageUri = imageUriForReview ?: return // Use a different name to avoid confusion

        _isOcrInProgress.value = true
        viewModelScope.launch {
            try {
                val preprocessedBitmap = preprocessImageForOcr(reviewedBitmap)
                val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val ocrText = visionText.text
                        _extractedText.value = ocrText

                        val documentTimestamp = extractDocumentDate(context, currentImageUri)
                        val parsedEntities = com.hereliesaz.lexorcist.DataParser.tagData(ocrText)
                        var evidenceToEmit = Evidence(
                            id = 0,
                            spreadsheetId = "", // Placeholder, to be set by the collector
                            content = ocrText,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = currentImageUri.toString(),
                            documentDate = documentTimestamp,
                            allegationId = null,
                            category = "OCR Image",
                            tags = parsedEntities.values.flatten().distinct()
                        )

                        val userScript = settingsManager.getScript()
                        if (userScript.isNotBlank()) {
                            when (val scriptResult = scriptRunner.runScript(userScript, evidenceToEmit)) {
                                is Result.Success -> {
                                    evidenceToEmit = evidenceToEmit.copy(tags = (evidenceToEmit.tags + scriptResult.data.tags).distinct())
                                }
                                is Result.Error -> {
                                    Log.e(tag, "Error running user script on OCR evidence", scriptResult.exception)
                                }
                            }
                        }

                        viewModelScope.launch {
                            _newlyCreatedEvidence.emit(evidenceToEmit)
                        }
                        _isOcrInProgress.value = false
                        cancelImageReview() // Clear review state
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Text recognition failed", e)
                        _isOcrInProgress.value = false
                        cancelImageReview()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error during OCR processing in confirmImageReview", e)
                _isOcrInProgress.value = false
                cancelImageReview()
            }
        }
    }

    private fun extractDocumentDate(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dateTimeString ->
                    // EXIF format is "yyyy:MM:dd HH:mm:ss"
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC") // Or local, if EXIF is local
                    sdf.parse(dateTimeString)?.time
                }
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(tag, "Could not extract EXIF date from $uri, using current time.", e)
            System.currentTimeMillis()
        }
    }

    fun cancelImageReview() {
        _imageBitmapForReview.value = null
        imageUriForReview = null
    }

    fun performOcrOnUri(uri: Uri, context: Context, caseIdInput: Int, parentVideoIdInput: String?) {
        _isOcrInProgress.value = true
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri, context)
                if (bitmap == null) {
                    Log.e(tag, "Failed to load bitmap from URI for OCR: $uri")
                    _isOcrInProgress.value = false
                    return@launch
                }

                val preprocessedBitmap = preprocessImageForOcr(bitmap)
                val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val ocrText = visionText.text
                        _extractedText.value = ocrText
                        
                        val documentTimestamp = extractDocumentDate(context, uri)
                        val parsedEntities = com.hereliesaz.lexorcist.DataParser.tagData(ocrText)

                        val evidenceToEmit = Evidence(
                            id = 0,
                            spreadsheetId = "", // Placeholder, caseIdInput is ignored for now
                            content = ocrText,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = uri.toString(),
                            documentDate = documentTimestamp,
                            allegationId = null,
                            category = "OCR from Video",
                            tags = parsedEntities.values.flatten().distinct()
                        )
                        // Note: ScriptRunner could also be applied here if needed
                        viewModelScope.launch {
                            _newlyCreatedEvidence.emit(evidenceToEmit)
                        }
                        _isOcrInProgress.value = false
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Text recognition failed for performOcrOnUri", e)
                        _isOcrInProgress.value = false
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error during OCR processing in performOcrOnUri", e)
                _isOcrInProgress.value = false
            }
        }
    }

    fun updateExtractedText(text: String) {
        _extractedText.value = text
    }

    private fun preprocessImageForOcr(bitmap: Bitmap): Bitmap {
        // Consider making preprocessing optional or configurable via SettingsManager
        // For now, keeping the existing logic
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        // ... (rest of preprocessing logic remains the same) ...
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)

        // Optional: Deskewing - can be complex and depends on image characteristics
        // val deskewedMat = deskewImage(mat) // Assuming deskewImage is a new private method

        val binaryMat = Mat()
        Imgproc.adaptiveThreshold(
            blurred, // Use blurred or deskewedMat if implemented
            binaryMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11, // Block size
            2.0  // C value
        )
        val resultBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(binaryMat, resultBitmap)
        return resultBitmap
    }
}
