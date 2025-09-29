package com.hereliesaz.lexorcist.data

import com.google.gson.annotations.SerializedName

/**
 * Data classes specifically for parsing the `evidence_catalog.json` file.
 * These are kept separate from `Allegation.kt` to avoid conflicts and ensure
 * each data model precisely matches its corresponding JSON file structure.
 */

// Represents the root object of the evidence_catalog.json file
data class EvidenceCatalogRoot(
    val description: String,
    @SerializedName("allegation_evidence_catalog")
    val allegationEvidenceCatalog: AllegationEvidenceCatalog
)

// Represents the nested object containing lists of civil and criminal allegations
data class AllegationEvidenceCatalog(
    @SerializedName("civil_allegations")
    val civilAllegations: List<EvidenceCatalogEntry>,
    @SerializedName("criminal_allegations")
    val criminalAllegations: List<EvidenceCatalogEntry>,
    val testimonial: List<String>,
    val digital: List<String>
)

// Represents a single allegation entry within the evidence_catalog.json file
data class EvidenceCatalogEntry(
    val id: String,
    val allegationName: String,
    @SerializedName("relevant_evidence")
    val relevantEvidence: Map<String, List<String>>
)