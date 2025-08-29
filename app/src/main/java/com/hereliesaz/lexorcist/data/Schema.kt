package com.hereliesaz.lexorcist.data

object Schema {
    const val APP_ROOT_FOLDER_NAME = "The Lexorcist"
    const val CASE_REGISTRY_SPREADSHEET_NAME = "Lexorcist_Case_Registry"

    object CaseRegistry {
        const val SHEET_NAME = "Cases"
        const val COL_CASE_NAME = "Case Name"
        const val COL_SPREADSHEET_ID = "Spreadsheet ID"
        const val COL_GENERATED_PDF_ID = "Generated PDF ID"
        const val COL_SOURCE_HTML_SNAPSHOT_ID = "Source HTML Snapshot ID"
        const val COL_ORIGINAL_MASTER_HTML_TEMPLATE_ID = "Original Master HTML Template ID"
        val HEADER = listOf(
            COL_CASE_NAME,
            COL_SPREADSHEET_ID,
            COL_GENERATED_PDF_ID,
            COL_SOURCE_HTML_SNAPSHOT_ID,
            COL_ORIGINAL_MASTER_HTML_TEMPLATE_ID
        )
    }

    object Allegations {
        const val SHEET_NAME = "Allegations"
        const val COL_ALLEGATION_TEXT = "Allegation"
        val HEADER = listOf(COL_ALLEGATION_TEXT)
    }

    object Evidence {
        const val SHEET_NAME = "Evidence"
        const val COL_CONTENT = "Content"
        const val COL_TIMESTAMP = "Timestamp"
        const val COL_SOURCE_DOCUMENT = "Source Document"
        const val COL_DOCUMENT_DATE = "Document Date"
        const val COL_TAGS = "Tags"
        const val COL_ALLEGATION_ID = "Allegation ID"
        const val COL_CATEGORY = "Category"
        val HEADER = listOf(
            COL_CONTENT,
            COL_TIMESTAMP,
            COL_SOURCE_DOCUMENT,
            COL_DOCUMENT_DATE,
            COL_TAGS,
            COL_ALLEGATION_ID,
            COL_CATEGORY
        )
    }
}
