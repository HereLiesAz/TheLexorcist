package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.CleanupSuggestion
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.hereliesaz.lexorcist.ui.components.DragAndDropContainer
import com.hereliesaz.lexorcist.ui.components.DraggableItem
import com.hereliesaz.lexorcist.ui.components.DropTarget

private class DragInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggableComposable: (@Composable () -> Unit)? by mutableStateOf(null)
    var dataToDrop: Any? by mutableStateOf(null)
}

private val LocalDragInfo = staticCompositionLocalOf { DragInfo() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitsScreen(caseViewModel: CaseViewModel = hiltViewModel()) {
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val isLoading by caseViewModel.isLoading.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("View", "Clean Up", "Assign")

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            caseViewModel.loadExhibits()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.exhibits).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.hereliesaz.azload.CoinTossLoadingIndicator()
                    }
                } else if (selectedCase == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.please_select_case_for_exhibits).uppercase(Locale.getDefault()))
                    }
                } else {
                    when (tabIndex) {
                        0 -> ViewTab(caseViewModel)
                        1 -> CleanUpTab(caseViewModel)
                        2 -> AssignTab(caseViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewTab(caseViewModel: CaseViewModel) {
    val exhibits by caseViewModel.exhibits.collectAsState()

    if (exhibits.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.no_exhibits_for_case).uppercase(Locale.getDefault()))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
            items(exhibits) { exhibit ->
                ExhibitDisplayItem(exhibit = exhibit)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.exhibits).uppercase(Locale.getDefault()),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            },
        ) { padding ->
            Column(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.hereliesaz.azload.CoinTossLoadingIndicator()
                    }
                } else if (selectedCase == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.please_select_case_for_exhibits).uppercase(Locale.getDefault()))
                    }
                } else {
                    when (selectedTab) {
                        "Exhibits" -> {
                            if (exhibits.isEmpty()) {
                                Text(stringResource(R.string.no_exhibits_for_case).uppercase(Locale.getDefault()))
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(exhibits) { exhibit ->
                                        ExhibitItem(
                                            exhibit = exhibit,
                                            onDeleteClick = { caseViewModel.deleteExhibit(it) }
                                        )
                                    }
                                }
                            }
                        }
                        "Clean Up" -> {
                            CleanUpTab(caseViewModel = caseViewModel)
                        }
                        "Assign" -> {
                            CompositionLocalProvider(LocalDragInfo provides remember { DragInfo() }) {
                                AssignTab(caseViewModel = caseViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanUpTab(caseViewModel: CaseViewModel) {
    val cleanupSuggestions by caseViewModel.cleanupSuggestions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AzButton(
            onClick = { caseViewModel.generateCleanupSuggestions() },
            text = stringResource(id = R.string.scan_for_cleanup)
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cleanupSuggestions) { suggestion ->
                when (suggestion) {
                    is CleanupSuggestion.DuplicateGroup -> {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Duplicate Group Found:", style = MaterialTheme.typography.titleMedium)
                            suggestion.evidence.forEach {
                                Text(" - ${it.sourceDocument} (ID: ${it.id})")
                            }
                            AzButton(
                                onClick = { caseViewModel.deleteDuplicates(suggestion) },
                                text = "Delete Duplicates (Keep 1)"
                            )
                        }
                    }
                    is CleanupSuggestion.ImageSeriesGroup -> {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Image Series Found:", style = MaterialTheme.typography.titleMedium)
                            suggestion.evidence.forEach {
                                Text(" - ${it.sourceDocument} (ID: ${it.id})")
                            }
                            AzButton(
                                onClick = { caseViewModel.mergeImageSeries(suggestion, "Merged Series") },
                                text = "Merge Image Series"
                            )
                        }
                    }
                }
                Divider()
            }
        }
    }
}

@Composable
fun AssignTab(caseViewModel: CaseViewModel) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    val dragInfo = LocalDragInfo.current
    val dropTargets = remember { mutableStateMapOf<Any, Rect>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                items(evidenceList.filter { ev -> exhibits.none { ex -> ex.evidenceIds.contains(ev.id) } }) { evidence ->
                    Draggable(
                        dataToDrop = evidence,
                        onDragEnd = {
                            dropTargets.forEach { (exhibitId, rect) ->
                                if (rect.contains(dragInfo.dragPosition + dragInfo.dragOffset)) {
                                    caseViewModel.assignEvidenceToDynamicExhibit((exhibits.find { it.id == exhibitId }?.name ?: ""), evidence.id)
                                    return@Draggable
                                }
                            }
                        }
                    ) {
                        Card(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
                            Text(text = evidence.content.take(100), modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                items(exhibits) { exhibit ->
                    DropTarget(
                        modifier = Modifier.onGloballyPositioned {
                            dropTargets[exhibit.id] = it.boundsInRoot()
                        }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = exhibit.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = exhibit.description)
                            exhibit.evidenceIds.forEach { evidenceId ->
                                Text(text = " - Evidence ID: $evidenceId", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (dragInfo.isDragging) {
            var targetSize by remember { mutableStateOf(IntSize.Zero) }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { targetSize = it.size }
                    .graphicsLayer {
                        val offset = (dragInfo.dragPosition + dragInfo.dragOffset)
                        alpha = if (targetSize == IntSize.Zero) 0f else .9f
                        translationX = offset.x.minus(targetSize.width / 2)
                        translationY = offset.y.minus(targetSize.height / 2)
                    }
            ) {
                dragInfo.draggableComposable?.invoke()
            }
        }
    }
}



@Composable
fun Draggable(
    modifier: Modifier = Modifier,
    dataToDrop: Any,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    val dragInfo = LocalDragInfo.current
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragInfo.isDragging = true
                        dragInfo.dataToDrop = dataToDrop
                        dragInfo.dragPosition = it
                        dragInfo.draggableComposable = content
                    },
                    onDragEnd = {
                        onDragEnd()
                        dragInfo.isDragging = false
                        dragInfo.dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragInfo.dragOffset += dragAmount
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun DropTarget(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val dragInfo = LocalDragInfo.current
    val isCurrentDropTarget = dragInfo.isDragging &&
            modifier.onGloballyPositioned {}.let {
                val bounds = remember { mutableStateOf(Rect.Zero) }
                Modifier.onGloballyPositioned { layoutCoordinates ->
                    bounds.value = layoutCoordinates.boundsInRoot()
                }
                bounds.value.contains(dragInfo.dragPosition + dragInfo.dragOffset)
            }

    Box(modifier = modifier.background(if (isCurrentDropTarget) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
        content()
    }
}


@Composable
fun ExhibitItem(
    exhibit: Exhibit,
    onDeleteClick: (Exhibit) -> Unit,
) {
    Card(
        modifier =
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exhibit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = exhibit.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = { onDeleteClick(exhibit) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete).uppercase(Locale.getDefault()))
            }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = exhibit.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = exhibit.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CleanUpTab(caseViewModel: CaseViewModel) {
    val cleanupSuggestions by caseViewModel.cleanupSuggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LexorcistOutlinedButton(
            onClick = { caseViewModel.generateCleanupSuggestions() },
            text = "Scan for Cleanup Suggestions"
        )

        if (cleanupSuggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cleanupSuggestions) { suggestion ->
                    when (suggestion) {
                        is CleanupSuggestion.DuplicateGroup -> {
                            DuplicateGroupItem(
                                group = suggestion,
                                onMerge = { caseViewModel.deleteDuplicates(suggestion) }
                            )
                        }
                        is CleanupSuggestion.ImageSeriesGroup -> {
                            ImageSeriesGroupItem(
                                group = suggestion,
                                onMerge = { caseViewModel.mergeImageSeries(suggestion, "Merged Series") }
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "No cleanup suggestions. Run a scan to find duplicates or image series.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
fun DuplicateGroupItem(group: CleanupSuggestion.DuplicateGroup, onMerge: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Duplicate Group Found (${group.evidence.size} items)", style = MaterialTheme.typography.titleMedium)
            group.evidence.forEach {
                Text(" - Evidence ID: ${it.id}, Content: ${it.content.take(50)}...", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LexorcistOutlinedButton(onClick = onMerge, text = "Merge (Keep First, Delete Others)")
        }
    }
}

@Composable
fun ImageSeriesGroupItem(group: CleanupSuggestion.ImageSeriesGroup, onMerge: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Image Series Found (${group.evidence.size} items)", style = MaterialTheme.typography.titleMedium)
            group.evidence.forEach {
                Text(" - Evidence ID: ${it.id}, Source: ${it.sourceDocument}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LexorcistOutlinedButton(onClick = onMerge, text = "Combine into PDF")
        }
    }
}

@Composable
fun AssignTab(caseViewModel: CaseViewModel) {
    val pertinentExhibitTypes by caseViewModel.pertinentExhibitTypes.collectAsState()
    val allEvidence by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()

    val assignedEvidenceIds = exhibits.flatMap { it.evidenceIds }.toSet()
    val unassignedEvidence = allEvidence.filter { it.id !in assignedEvidenceIds }

    DragAndDropContainer<Evidence> {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Panel: Pertinent Exhibit Types
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pertinent Exhibits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pertinentExhibitTypes) { exhibitType ->
                        DropTarget<Evidence>(
                            key = exhibitType,
                            onDropped = { evidence ->
                                caseViewModel.assignEvidenceToDynamicExhibit(evidence.id, exhibitType)
                            }
                        ) { isHovered ->
                           ExhibitTypeDisplayItem(exhibitType, isHovered)
                        }
                    }
                }
            }

            // Right Panel: Unassigned Evidence
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Unassigned Evidence", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unassignedEvidence) { evidence ->
                        DraggableItem(dataToDrop = evidence) {
                            EvidenceDisplayItem(evidence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExhibitTypeDisplayItem(exhibitType: String, isHovered: Boolean) {
    val color = if (isHovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = exhibitType,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EvidenceDisplayItem(evidence: Evidence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Type: ${evidence.type}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = evidence.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}