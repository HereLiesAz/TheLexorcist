package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.pie.rememberPieChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.pie.PieEntry

@Composable
fun VisualizationScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()

    val chartEntryModelProducer = ChartEntryModelProducer()
    val entries = evidenceList
        .groupBy { it.category }
        .map { (category, evidence) ->
            PieEntry(evidence.size.toFloat(), category)
        }
    chartEntryModelProducer.setEntries(entries)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Evidence by Category", style = MaterialTheme.typography.headlineMedium)
        Chart(
            chart = rememberPieChart(),
            chartModelProducer = chartEntryModelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
        )
    }
}
