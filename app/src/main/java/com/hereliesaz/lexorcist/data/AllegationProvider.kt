package com.hereliesaz.lexorcist.data

object AllegationProvider {

    private val personalInjuryElements = listOf(
        AllegationElement("Duty", "The defendant owed a legal duty to the plaintiff."),
        AllegationElement("Breach", "The defendant breached that legal duty."),
        AllegationElement("Causation", "The defendant's breach of duty caused the plaintiff's injury."),
        AllegationElement("Damages", "The plaintiff suffered actual damages as a result.")
    )

    private val breachOfContractElements = listOf(
        AllegationElement("Contract", "A valid contract existed between the parties."),
        AllegationElement("Performance", "The plaintiff performed their duties under the contract."),
        AllegationElement("Breach", "The defendant failed to perform their duties under the contract."),
        AllegationElement("Damages", "The plaintiff suffered damages as a result of the defendant's breach.")
    )

    private val allAllegations = listOf(
        Allegation(
            id = "personal_injury_negligence".hashCode(),
            spreadsheetId = "personal_injury_negligence",
            text = "Personal Injury (based on Negligence)",
            elements = personalInjuryElements
        ),
        Allegation(
            id = "breach_of_contract".hashCode(),
            spreadsheetId = "breach_of_contract",
            text = "Breach of Contract",
            elements = breachOfContractElements
        ),
        Allegation(
            id = "small_claims".hashCode(),
            spreadsheetId = "small_claims",
            text = "Small Claims",
            elements = emptyList() // No predefined elements for small claims
        ),
        Allegation(
            id = "eviction".hashCode(),
            spreadsheetId = "eviction",
            text = "Eviction (Rule for Possession)",
            elements = emptyList() // No predefined elements for eviction
        ),
        Allegation(
            id = "theft".hashCode(),
            spreadsheetId = "theft",
            text = "Theft",
            elements = emptyList() // No predefined elements for theft
        ),
        Allegation(
            id = "assault".hashCode(),
            spreadsheetId = "assault",
            text = "Assault (Simple & Aggravated)",
            elements = emptyList() // No predefined elements for assault
        ),
        Allegation(
            id = "civil_rights_1983".hashCode(),
            spreadsheetId = "civil_rights_1983",
            text = "Federal Civil Rights Violation (42 U.S.C. § 1983)",
            elements = emptyList() // No predefined elements for civil rights
        )
    )

    fun getAllAllegations(): List<Allegation> {
        return allAllegations
    }

    fun getAllegationById(id: Int): Allegation? {
        return allAllegations.find { it.id == id }
    }
}
