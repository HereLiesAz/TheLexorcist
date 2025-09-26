package com.hereliesaz.lexorcist.data

fun Evidence.toSheetRow(): List<Any> =
    listOf(
        id.toString(),
        caseId.toString(),
        type ?: "",
        content,
        timestamp.toString(),
        sourceDocument ?: "",
        documentDate.toString(),
        allegationId?.toString() ?: "",
        category ?: "",
        tags.joinToString(","),
        commentary ?: "",
        linkedEvidenceIds.joinToString(","),
        parentVideoId ?: "",
    )
