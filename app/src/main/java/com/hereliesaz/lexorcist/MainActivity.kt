package com.hereliesaz.lexorcist

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.db.FinancialEntry
import com.hereliesaz.lexorcist.db.SortOrder
import com.hereliesaz.lexorcist.ui.theme.LexorcistTheme
import java.util.Calendar
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity


class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var tempBitmap: Bitmap? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
            tempBitmap = bitmap
            viewModel.runTextRecognition(bitmap, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LexorcistTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSelectImage = { selectImageLauncher.launch("image/*") },
                    bitmap = tempBitmap,
                    showDateRangePicker = {
                        showDateRangePicker()
                    }
                )
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.setDateRange(it.first, it.second)
        }
        picker.show(supportFragmentManager, picker.toString())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSelectImage: () -> Unit,
    bitmap: Bitmap?,
    showDateRangePicker: () -> Unit
) {
    val entries by viewModel.entries.observeAsState(initial = emptyList())
    val totalAmount by viewModel.totalAmount.observeAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lexorcist") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onSelectImage) {
                Text("Select Image to Scan")
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SortAndFilter(viewModel, showDateRangePicker)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(entries) { entry ->
                    FinancialEntryRow(entry)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = totalAmount, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun SortAndFilter(viewModel: MainViewModel, showDateRangePicker: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortOptions = listOf(
        "Sort by Date (Newest)",
        "Sort by Date (Oldest)",
        "Sort by Amount (High to Low)",
        "Sort by Amount (Low to High)"
    )
    var selectedSort by remember { mutableStateOf(sortOptions[0]) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Button(onClick = { expanded = true }) {
                Text(selectedSort)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortOptions.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            selectedSort = text
                            expanded = false
                            val sortOrder = when (index) {
                                0 -> SortOrder.DATE_DESC
                                1 -> SortOrder.DATE_ASC
                                2 -> SortOrder.AMOUNT_DESC
                                else -> SortOrder.AMOUNT_ASC
                            }
                            viewModel.setSortOrder(sortOrder)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = showDateRangePicker) {
            Text("Filter")
        }
    }
}

@Composable
fun FinancialEntryRow(entry: FinancialEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.amount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(entry.documentDate)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

class MockApplication : Application()

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LexorcistTheme {
        MainScreen(
            viewModel = MainViewModel(MockApplication()),
            onSelectImage = {},
            bitmap = null,
            showDateRangePicker = {}
        )
    }
}
