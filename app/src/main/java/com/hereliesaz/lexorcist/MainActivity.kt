package com.hereliesaz.lexorcist

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
import com.hereliesaz.lexorcist.db.AppDatabase
import com.hereliesaz.lexorcist.db.FinancialEntry
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
import com.hereliesaz.lexorcist.db.SortOrder
import java.text.SimpleDateFormat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.SignInButton
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import java.util.Collections
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {

    private lateinit var selectImageButton: Button
    private lateinit var imageView: ImageView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var totalAmountTextView: TextView
    private lateinit var entryAdapter: FinancialEntryAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var filterButton: Button
    private lateinit var signInButton: SignInButton
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var createCaseButton: Button
    private lateinit var loadCaseButton: Button
    private lateinit var saveToSheetButton: Button
    private lateinit var editScriptButton: Button
    private var driveService: Drive? = null
    private var sheetsService: Sheets? = null
    private var currentSpreadsheetId: String? = null

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var currentSortOrder = SortOrder.DATE_DESC
    private var startDate: Long? = null
    private var endDate: Long? = null


    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

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
        signInButton = findViewById(R.id.sign_in_button)
        createCaseButton = findViewById(R.id.create_case_button)
        loadCaseButton = findViewById(R.id.load_case_button)
        saveToSheetButton = findViewById(R.id.save_to_sheet_button)
        editScriptButton = findViewById(R.id.edit_script_button)

        setupRecyclerView()
        setupSortSpinner()
        setupFilterButton()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"), Scope("https://www.googleapis.com/auth/spreadsheets"))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInButton.setOnClickListener {
            signIn()
        }

        selectImageButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        createCaseButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("New Case Name")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                val caseName = input.text.toString()
                if (caseName.isNotEmpty()) {
                    createSpreadsheet(caseName)
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }

        loadCaseButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = driveService?.files()?.list()
                        ?.setQ("mimeType='application/vnd.google-apps.spreadsheet'")
                        ?.setSpaces("drive")
                        ?.setFields("nextPageToken, files(id, name)")
                        ?.execute()

                    val files = result?.files
                    if (files.isNullOrEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "No spreadsheets found.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val items = files.map { it.name }.toTypedArray()
                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Select a Case")
                                .setItems(items) { _, which ->
                                    currentSpreadsheetId = files[which].id
                                    Toast.makeText(this@MainActivity, "Loaded case: ${items[which]}", Toast.LENGTH_LONG).show()
                                }
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to load cases.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        saveToSheetButton.setOnClickListener {
            if (currentSpreadsheetId != null) {
                saveDataToSheet()
            } else {
                Toast.makeText(this, "Please create or load a case first.", Toast.LENGTH_LONG).show()
            }
        }

        editScriptButton.setOnClickListener {
            if (currentSpreadsheetId != null) {
                val intent = android.content.Intent(this, ScriptEditorActivity::class.java)
                intent.putExtra("spreadsheetId", currentSpreadsheetId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please create or load a case first.", Toast.LENGTH_LONG).show()
            }
        }

        loadAndDisplayEntriesAndTotal()
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // The user is already signed in, show the authenticated UI
            signInButton.visibility = View.GONE
            selectImageButton.visibility = View.VISIBLE
            createCaseButton.visibility = View.VISIBLE
            loadCaseButton.visibility = View.VISIBLE
            saveToSheetButton.visibility = View.VISIBLE
            editScriptButton.visibility = View.VISIBLE
        } else {
            // The user is not signed in, show the sign-in button
            signInButton.visibility = View.VISIBLE
            selectImageButton.visibility = View.GONE
            createCaseButton.visibility = View.GONE
            loadCaseButton.visibility = View.GONE
            saveToSheetButton.visibility = View.GONE
            editScriptButton.visibility = View.GONE
        }
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
            val categories = arrayOf("Food", "Transportation", "Utilities", "Entertainment", "Other")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a category")
            builder.setItems(categories) { _, which ->
                val selectedCategory = categories[which]
                lifecycleScope.launch {
                    amounts.forEach { amount ->
                        val entry = FinancialEntry(
                            amount = amount,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = uri.toString(),
                            documentDate = documentDate,
                            category = selectedCategory
                        )
                        db.financialEntryDao().insert(entry)
                    }
                    Log.d("DB", "Inserted ${amounts.size} entries with category $selectedCategory.")
                    loadAndDisplayEntriesAndTotal()
                }
            }
            builder.show()
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

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
            signInButton.visibility = View.GONE
            selectImageButton.visibility = View.VISIBLE
            createCaseButton.visibility = View.VISIBLE
            loadCaseButton.visibility = View.VISIBLE
            saveToSheetButton.visibility = View.VISIBLE
            editScriptButton.visibility = View.VISIBLE

            // Initialize the Drive and Sheets services
            val credential = GoogleAccountCredential.usingOAuth2(
                this, setOf("https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/spreadsheets")
            )
            credential.selectedAccountName = account.account?.name
            driveService = Drive.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(getString(R.string.app_name)).build()
            sheetsService = Sheets.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(getString(R.string.app_name)).build()
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("SignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createSpreadsheet(title: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = title
            fileMetadata.mimeType = "application/vnd.google-apps.spreadsheet"

            try {
                val file = driveService?.files()?.create(fileMetadata)?.setFields("id")?.execute()
                file?.id?.let {
                    currentSpreadsheetId = it
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Case created with ID: $it", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to create case", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveDataToSheet() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Get all entries from the local database
                val entries = db.financialEntryDao().getAllEntries()

                // 2. Group entries by category
                val groupedEntries = entries.groupBy { it.category }

                // 3. For each category, create a sheet and add the data
                for ((category, categoryEntries) in groupedEntries) {
                    // Check if sheet exists
                    val spreadsheet = sheetsService?.spreadsheets()?.get(currentSpreadsheetId!!)?.execute()
                    val sheetExists = spreadsheet?.sheets?.any { it.properties.title == category } ?: false

                    if (!sheetExists) {
                        val addSheetRequest = com.google.api.services.sheets.v4.model.AddSheetRequest()
                        addSheetRequest.properties = com.google.api.services.sheets.v4.model.SheetProperties().setTitle(category)
                        val batchUpdateRequest = com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                            .setRequests(listOf(com.google.api.services.sheets.v4.model.Request().setAddSheet(addSheetRequest)))
                        sheetsService?.spreadsheets()?.batchUpdate(currentSpreadsheetId!!, batchUpdateRequest)?.execute()
                    }

                    // Append data
                    val values = categoryEntries.map { entry ->
                        listOf(entry.amount, Date(entry.documentDate).toString(), entry.sourceDocument)
                    }
                    val body = com.google.api.services.sheets.v4.model.ValueRange().setValues(values)
                    sheetsService?.spreadsheets()?.values()?.append(currentSpreadsheetId!!, category, body)
                        ?.setValueInputOption("USER_ENTERED")
                        ?.execute()
                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Data saved to sheet.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to save data.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
