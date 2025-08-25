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

class MainActivity : AppCompatActivity() {

    private lateinit var selectImageButton: Button
    private lateinit var imageView: ImageView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var totalAmountTextView: TextView
    private lateinit var entryAdapter: FinancialEntryAdapter

    private val db by lazy { AppDatabase.getDatabase(this) }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
            runTextRecognition(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectImageButton = findViewById(R.id.select_image_button)
        imageView = findViewById(R.id.image_view)
        entriesRecyclerView = findViewById(R.id.entries_recyclerview)
        totalAmountTextView = findViewById(R.id.total_amount_textview)

        setupRecyclerView()

        selectImageButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        loadAndDisplayEntriesAndTotal()
    }

    private fun setupRecyclerView() {
        entryAdapter = FinancialEntryAdapter()
        entriesRecyclerView.adapter = entryAdapter
        entriesRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Toast.makeText(this, "Text recognized!", Toast.LENGTH_SHORT).show()
                parseTextAndSaveData(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Error recognizing text", e)
                Toast.makeText(this, "Error recognizing text", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseTextAndSaveData(text: String) {
        val amounts = DataParser.parseAmounts(text)

        if (amounts.isNotEmpty()) {
            lifecycleScope.launch {
                amounts.forEach { amount ->
                    val entry = FinancialEntry(amount = amount, timestamp = System.currentTimeMillis())
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
            val entries = db.financialEntryDao().getAllEntries()
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
