package com.hereliesaz.lexorcist.data

object AllegationProvider {

    fun getAllAllegations(): List<Allegation> {
        return listOf(
            Allegation(
                id = "personal_injury_negligence".hashCode(),
                spreadsheetId = "personal_injury_negligence",
                text = "Personal Injury (based on Negligence)"
            ),
            Allegation(
                id = "breach_of_contract".hashCode(),
                spreadsheetId = "breach_of_contract",
                text = "Breach of Contract"
            ),
            Allegation(
                id = "small_claims".hashCode(),
                spreadsheetId = "small_claims",
                text = "Small Claims"
            ),
            Allegation(
                id = "eviction".hashCode(),
                spreadsheetId = "eviction",
                text = "Eviction (Rule for Possession)"
            ),
            Allegation(
                id = "theft".hashCode(),
                spreadsheetId = "theft",
                text = "Theft"
            ),
            Allegation(
                id = "assault".hashCode(),
                spreadsheetId = "assault",
                text = "Assault (Simple & Aggravated)"
            ),
            Allegation(
                id = "civil_rights_1983".hashCode(),
                spreadsheetId = "civil_rights_1983",
                text = "Federal Civil Rights Violation (42 U.S.C. § 1983)"
            )
        )
    }
}

// This data class might be used elsewhere, so keep its definition for now.
// However, it's not part of the Allegation object itself anymore.
data class AllegationElement(
    val name: String,
    val description: String
)