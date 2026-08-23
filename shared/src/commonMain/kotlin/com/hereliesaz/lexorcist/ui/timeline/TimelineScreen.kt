package com.hereliesaz.lexorcist.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.domain.model.DateSource
import com.hereliesaz.lexorcist.domain.model.Evidence
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The case chronology.
 *
 * The Android predecessor sorted by `documentDate` and formatted it with
 * `"MMM dd, yyyy"` / `"hh:mm a"` for every item. Because the date-extraction
 * regexes never matched and screenshots carry no EXIF `DateTimeOriginal`, that
 * field was `System.currentTimeMillis()` for most evidence -- so the screen
 * presented the order things were scanned as the order they happened, and
 * printed a fabricated time to the minute with nothing to signal it.
 *
 * Here, evidence whose date could not be established is separated out into an
 * explicitly labelled "Date not established" section rather than being placed
 * on the timeline at a made-up position, and a dated entry shows where its
 * date came from.
 */
@Composable
fun TimelineScreen(
    evidence: List<Evidence>,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onEvidenceClick: (Evidence) -> Unit = {},
) {
    val (dated, undated) = remember(evidence) {
        val d = evidence.filter { it.hasReliableDate }.sortedBy { it.documentDate }
        val u = evidence.filterNot { it.hasReliableDate }.sortedBy { it.ingestedAt }
        d to u
    }

    if (evidence.isEmpty()) {
        EmptyTimeline(modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(dated, key = { it.id.value }) { item ->
            TimelineEntry(item, timeZone, onClick = { onEvidenceClick(item) })
        }

        if (undated.isNotEmpty()) {
            item(key = "__undated_header__") {
                UndatedHeader(count = undated.size)
            }
            items(undated, key = { it.id.value }) { item ->
                TimelineEntry(item, timeZone, onClick = { onEvidenceClick(item) })
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    item: Evidence,
    timeZone: TimeZone,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .padding(top = 6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (item.hasReliableDate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    content = {},
                    modifier = Modifier.size(10.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                DateLine(item, timeZone)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.content.take(240).ifBlank { "(no extracted text)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.tags.joinToString("  ") { "#${it.label}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateLine(item: Evidence, timeZone: TimeZone) {
    val date = item.documentDate
    if (date == null || !item.hasReliableDate) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Date not established",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        return
    }

    Column {
        Text(
            text = formatDate(date, timeZone),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.documentDateSource.describe(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun UndatedHeader(count: Int) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Date not established ($count)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "These items could not be placed on the timeline. Set a date on each " +
                "to include it in the chronology.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun EmptyTimeline(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No evidence yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Capture or import evidence and dated items will appear here in order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun DateSource.describe(): String = when (this) {
    DateSource.FileMetadata -> "from file metadata"
    DateSource.ExtractedText -> "read from the document text"
    DateSource.SourceSystem -> "from the source system"
    DateSource.UserProvided -> "entered manually"
    DateSource.Unknown -> "source unknown"
}

private val monthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

internal fun formatDate(instant: Instant, timeZone: TimeZone): String {
    val dt = instant.toLocalDateTime(timeZone)
    val month = monthNames[dt.monthNumber - 1]
    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    return "$month ${dt.day}, ${dt.year}  $hh:$mm"
}
