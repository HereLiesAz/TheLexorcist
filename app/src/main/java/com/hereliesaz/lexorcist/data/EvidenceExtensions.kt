package com.hereliesaz.lexorcist.data

fun Evidence.toSheetRow(): List<String> =
    listOf(
        id,
        caseId.toString(),
        type,
        content,
        timestamp.toString(),
        sourceDocument ?: "",
        documentDate.toString(),
        allegationId ?: "",
        category,
        tags.joinToString(","),
        commentary ?: "",
        linkedEvidenceIds.joinToString(","),
        parentVideoId ?: "",
    )
