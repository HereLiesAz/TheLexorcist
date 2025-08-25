package com.hereliesaz.lexorcist

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.db.AppDatabase
import com.hereliesaz.lexorcist.db.FinancialEntry
import com.hereliesaz.lexorcist.db.SortOrder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { AppDatabase.getDatabase(application) }

    private val _entries = MutableLiveData<List<FinancialEntry>>()
    val entries: LiveData<List<FinancialEntry>> = _entries

    private val _totalAmount = MutableLiveData<String>()
    val totalAmount: LiveData<String> = _totalAmount

    private var currentSortOrder = SortOrder.DATE_DESC
    private var startDate: Long? = null
    private var endDate: Long? = null

    init {
        loadAndDisplayEntriesAndTotal()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        currentSortOrder = sortOrder
        loadAndDisplayEntriesAndTotal()
    }

    fun setDateRange(startDate: Long, endDate: Long) {
        this.startDate = startDate
        this.endDate = endDate
        loadAndDisplayEntriesAndTotal()
    }

    fun clearDateRange() {
        this.startDate = null
        this.endDate = null
        loadAndDisplayEntriesAndTotal()
    }

    fun runTextRecognition(bitmap: Bitmap, uri: Uri) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Toast.makeText(getApplication(), "Text recognized!", Toast.LENGTH_SHORT).show()
                parseTextAndSaveData(visionText.text, uri)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Error recognizing text", e)
                Toast.makeText(getApplication(), "Error recognizing text", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseTextAndSaveData(text: String, uri: Uri) {
        val amounts = DataParser.parseAmounts(text)
        val dates = DataParser.parseDates(text)
        val documentDate = dates.firstOrNull() ?: System.currentTimeMillis()

        if (amounts.isNotEmpty()) {
            viewModelScope.launch {
                amounts.forEach { amount ->
                    val entry = FinancialEntry(
                        amount = amount,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = uri.toString(),
                        documentDate = documentDate
                    )
                    db.financialEntryDao().insert(entry)
                }
                Log.d("DB", "Inserted ${amounts.size} entries.")
                loadAndDisplayEntriesAndTotal()
            }
        } else {
            Toast.makeText(getApplication(), "No amounts found in image.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAndDisplayEntriesAndTotal() {
        viewModelScope.launch {
            val entriesList = db.financialEntryDao().getEntries(
                currentSortOrder,
                startDate ?: 0,
                endDate ?: Long.MAX_VALUE
            )
            _entries.postValue(entriesList)

            val total = entriesList.sumOf { entry ->
                val cleanString = entry.amount.replace(Regex("[^\\d.]"), "")
                cleanString.toDoubleOrNull() ?: 0.0
            }

            val currencyFormat = NumberFormat.getCurrencyInstance()
            currencyFormat.currency = Currency.getInstance(Locale.getDefault())
            _totalAmount.postValue("Total: ${currencyFormat.format(total)}")
        }
    }
}
