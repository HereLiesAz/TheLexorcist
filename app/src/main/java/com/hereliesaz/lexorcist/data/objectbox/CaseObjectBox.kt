package com.hereliesaz.lexorcist.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class CaseObjectBox(
    @Id
    var id: Long = 0,
    var name: String = "",
    var spreadsheetId: String = "",
    var scriptId: String? = null,
    var generatedPdfId: String? = null,
    var sourceHtmlSnapshotId: String? = null,
    var originalMasterHtmlTemplateId: String? = null,
    var folderId: String? = null,
    var plaintiffs: String? = null,
    var defendants: String? = null,
    var court: String? = null,
    var isArchived: Boolean = false,
    var lastModifiedTime: Long? = null
)
