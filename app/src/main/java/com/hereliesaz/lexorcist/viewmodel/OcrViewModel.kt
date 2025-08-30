package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.ArrayList

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val _isOcrInProgress = MutableStateFlow(false)
    val isOcrInProgress: StateFlow<Boolean> = _isOcrInProgress.asStateFlow()

    private val _imageBitmapForReview = MutableStateFlow<Bitmap?>(null)
    val imageBitmapForReview: StateFlow<Bitmap?> = _imageBitmapForReview.asStateFlow()

    private var imageUriForReview: Uri? = null

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()

    private val _newlyCreatedEvidence = MutableSharedFlow<Evidence>()
    val newlyCreatedEvidence = _newlyCreatedEvidence.asSharedFlow()

    fun startImageReview(uri: Uri, context: Context) {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(uri, context)
            if (bitmap != null) {
                imageUriForReview = uri
                _imageBitmapForReview.value = bitmap
            }
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun rotateImageBeingReviewed(degrees: Float) {
        val currentBitmap = _imageBitmapForReview.value ?: return
        viewModelScope.launch {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
            _imageBitmapForReview.value = rotatedBitmap
        }
    }

    fun confirmImageReview(context: Context) {
        val reviewedBitmap = _imageBitmapForReview.value ?: return
        val reviewedUri = imageUriForReview ?: return

        viewModelScope.launch {
            _isOcrInProgress.value = true
            try {
                val preprocessedBitmap = preprocessImageForOcr(reviewedBitmap)
                val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        _extractedText.value = visionText.text
                        val newEvidence = Evidence(
                            id = 0,
                            caseId = 0,
                            content = visionText.text,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = reviewedUri.toString(),
                            documentDate = System.currentTimeMillis(),
                            allegationId = null,
                            category = "OCR Image",
                            tags = emptyList()
                        )
                        viewModelScope.launch {
                            _newlyCreatedEvidence.emit(newEvidence)
                        }
                        _isOcrInProgress.value = false
                        cancelImageReview()
                    }
                    .addOnFailureListener { e ->
                        _isOcrInProgress.value = false
                        cancelImageReview()
                    }
            } catch (e: Exception) {
                _isOcrInProgress.value = false
                cancelImageReview()
            }
        }
    }

    fun cancelImageReview() {
        _imageBitmapForReview.value = null
        imageUriForReview = null
    }

    private fun preprocessImageForOcr(bitmap: Bitmap): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)

        val edged = Mat()
        Imgproc.Canny(blurred, edged, 75.0, 200.0)

        val contours = ArrayList<org.opencv.core.MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        if (contours.isNotEmpty()) {
            val boxPoints = contours.flatMap { it.toList() }
            if (boxPoints.isNotEmpty()) {
                val points = org.opencv.core.MatOfPoint2f(*boxPoints.toTypedArray())
                val rect = Imgproc.minAreaRect(points)

                var angle = rect.angle
                if (angle < -45) {
                    angle = -(90 + angle)
                } else {
                    angle = -angle
                }

                if (angle != 0.0) {
                    val center = rect.center
                    val rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0)
                    val rotatedMat = Mat()
                    Imgproc.warpAffine(mat, rotatedMat, rotationMatrix, mat.size(), Imgproc.INTER_CUBIC)
                    mat = rotatedMat
                }
            }
        }

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        val blurredMat = Mat()
        Imgproc.medianBlur(grayMat, blurredMat, 3)

        val binaryMat = Mat()
        Imgproc.adaptiveThreshold(
            blurredMat,
            binaryMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )

        val resultBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(binaryMat, resultBitmap)

        return resultBitmap
    }
}
