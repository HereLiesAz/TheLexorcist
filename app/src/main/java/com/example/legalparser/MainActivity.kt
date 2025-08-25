package com.example.legalparser

import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.legalparser.db.AppDatabase
import com.example.legalparser.db.FinancialEntry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

import android.app.DatePickerDialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.legalparser.db.SortOrder
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var selectImageButton: Button
    private lateinit var imageView: ImageView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var totalAmountTextView: TextView
    private lateinit var entryAdapter: FinancialEntryAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var filterButton: Button

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var currentSortOrder = SortOrder.DATE_DESC
    private var startDate: Long? = null
    private var endDate: Long? = null


    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
            runTextRecognition(bitmap, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectImageButton = findViewById(R.id.select_image_button)
        imageView = findViewById(R.id.image_view)
        entriesRecyclerView = findViewById(R.id.entries_recyclerview)
        totalAmountTextView = findViewById(R.id.total_amount_textview)
        sortSpinner = findViewById(R.id.sort_spinner)
        filterButton = findViewById(R.id.filter_button)

        setupRecyclerView()
        setupSortSpinner()
        setupFilterButton()

        selectImageButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        loadAndDisplayEntriesAndTotal()
    }

    private fun setupFilterButton() {
        filterButton.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, startYear, startMonth, startDay ->
            val startCalendar = Calendar.getInstance().apply { set(startYear, startMonth, startDay, 0, 0, 0) }
            startDate = startCalendar.timeInMillis

            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                val endCalendar = Calendar.getInstance().apply { set(endYear, endMonth, endDay, 23, 59, 59) }
                endDate = endCalendar.timeInMillis
                loadAndDisplayEntriesAndTotal()
            }, year, month, day).apply {
                setTitle("Select End Date")
                show()
            }
        }, year, month, day).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    private fun setupSortSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.sort_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortOrder = when (position) {
                    0 -> SortOrder.DATE_DESC
                    1 -> SortOrder.DATE_ASC
                    2 -> SortOrder.AMOUNT_DESC
                    else -> SortOrder.AMOUNT_ASC
                }
                loadAndDisplayEntriesAndTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        entryAdapter = FinancialEntryAdapter()
        entriesRecyclerView.adapter = entryAdapter
        entriesRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun runTextRecognition(bitmap: Bitmap, uri: Uri) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Toast.makeText(this, "Text recognized!", Toast.LENGTH_SHORT).show()
                parseTextAndSaveData(visionText.text, uri)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Error recognizing text", e)
                Toast.makeText(this, "Error recognizing text", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseTextAndSaveData(text: String, uri: Uri) {
        val amounts = DataParser.parseAmounts(text)
        val dates = DataParser.parseDates(text)
        val documentDate = dates.firstOrNull() ?: System.currentTimeMillis()

        if (amounts.isNotEmpty()) {
            lifecycleScope.launch {
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
            Toast.makeText(this, "No amounts found in image.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAndDisplayEntriesAndTotal() {
        lifecycleScope.launch {
            val entries = db.financialEntryDao().getEntries(
                currentSortOrder,
                startDate ?: 0,
                endDate ?: Long.MAX_VALUE
            )
            entryAdapter.submitList(entries)

            val total = entries.sumOf { entry ->
                // Clean the string and parse to Double
                val cleanString = entry.amount.replace(Regex("[^\\d.]"), "")
                cleanString.toDoubleOrNull() ?: 0.0
            }

            val currencyFormat = NumberFormat.getCurrencyInstance()
            totalAmountTextView.text = "Total: ${currencyFormat.format(total)}"
        }
    }
}
