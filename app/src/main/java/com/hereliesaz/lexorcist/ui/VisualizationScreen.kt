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
import androidx.compose.ui.res.stringResource // Added for Text
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R // Added for stringResource
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.rememberColumnChart
import com.patrykandpatrick.vico.compose.chart.pie.rememberPieChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.pie.PieEntry
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VisualizationScreen(evidenceViewModel: EvidenceViewModel) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()

    // Data for Pie Chart
    val pieChartEntryModelProducer = ChartEntryModelProducer<PieEntry>()
    val pieEntries: List<PieEntry> = evidenceList
        .groupBy { it.category } // Assuming it.category is a String
        .map { (category, evidenceInGroup) ->
            PieEntry(evidenceInGroup.size.toFloat(), category)
        }
    pieChartEntryModelProducer.setEntries(pieEntries)

    // Data for Bar Chart
    val barChartEntryModelProducer = ChartEntryModelProducer<FloatEntry>()
    val monthlyData = evidenceList
        .groupBy {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            sdf.format(it.timestamp)
        }
        .mapValues { it.value.size }

    val monthLabels = monthlyData.keys.toList()
    val barEntries: List<FloatEntry> = monthlyData.values.mapIndexed { index, count ->
        FloatEntry(index.toFloat(), count.toFloat())
    }
    barChartEntryModelProducer.setEntries(barEntries)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.evidence_by_category), style = MaterialTheme.typography.headlineMedium)
        Chart(
            chart = rememberPieChart(),
            chartModelProducer = pieChartEntryModelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
        )

        Text(stringResource(R.string.evidence_by_month), style = MaterialTheme.typography.headlineMedium)
        Chart(
            chart = rememberColumnChart(),
            chartModelProducer = barChartEntryModelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> monthLabels.getOrElse(value.toInt()) { "" } }
            ),
        )
    }
}
