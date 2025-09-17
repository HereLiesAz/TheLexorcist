package com.hereliesaz.lexorcist.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class SettingsObjectBox(
    @Id
    var id: Long = 1,
    var userScript: String = "",
    var storageLocation: String? = null,
    var theme: String = "System",
    var exportFormat: String = "PDF",
    var caseFolderPath: String? = null,
    var cloudSyncEnabled: Boolean = true,
    var selectedCloudProvider: String? = null // Added field
)
