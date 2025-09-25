package com.hereliesaz.lexorcist.data

object AllegationProvider {

    fun getAllAllegations(): List<Allegation> {
        return listOf(
            // Personal Injury (Negligence)
            Allegation(
                id = "personal_injury_negligence",
                text = "Personal Injury (based on Negligence)",
                description = "A legal action filed by a person who has suffered physical, emotional, or financial harm due to the carelessness or wrongful act of another.",
                elements = listOf(
                    AllegationElement("Duty of Care", "The defendant owed the plaintiff a legal duty to act with a certain level of care."),
                    AllegationElement("Breach of Duty", "The defendant failed to meet the standard of care required by their duty."),
                    AllegationElement("Causation", "The defendant's breach of duty was the actual and proximate cause of the plaintiff's injuries."),
                    AllegationElement("Damages", "The plaintiff suffered actual, legally compensable harm as a result of the injury.")
                ),
                evidenceSuggestions = listOf(
                    "Police or Accident Report",
                    "Medical Records and Bills",
                    "Photographs and Videos of scene/injuries",
                    "Witness Statements",
                    "Proof of Lost Wages",
                    "Expert Testimony (e.g., accident reconstructionist)"
                )
            ),
            // Breach of Contract
            Allegation(
                id = "breach_of_contract",
                text = "Breach of Contract",
                description = "A civil claim filed when one party to a legally binding agreement fails to honor its promises, causing financial harm to the other party.",
                elements = listOf(
                    AllegationElement("Existence of a Valid Contract", "A legally enforceable contract was formed (offer, acceptance, consideration)."),
                    AllegationElement("Plaintiff's Performance", "The plaintiff fulfilled their obligations under the contract."),
                    AllegationElement("Defendant's Breach", "The defendant failed to perform their contractual duties."),
                    AllegationElement("Resulting Damages", "The defendant's breach caused the plaintiff to suffer quantifiable financial losses.")
                ),
                evidenceSuggestions = listOf(
                    "The Written Contract",
                    "Correspondence (Emails, Texts, Letters)",
                    "Invoices, Receipts, and Proof of Payment",
                    "Photographs or Videos of work/goods",
                    "Witness Testimony"
                )
            ),
            // Small Claims
            Allegation(
                id = "small_claims",
                text = "Small Claims",
                description = "A simplified, informal, and inexpensive process for resolving civil disputes where the amount of money at stake is $5,000 or less.",
                elements = listOf(
                    AllegationElement("Jurisdictional Amount", "The amount in dispute must not exceed $5,000."),
                    AllegationElement("Valid Civil Claim", "The underlying allegation must be a recognizable civil cause of action."),
                    AllegationElement("Territorial Jurisdiction", "The defendant must be located on, or the incident must have occurred on, the Eastbank of Orleans Parish.")
                ),
                evidenceSuggestions = listOf(
                    "Written Agreements",
                    "Financial Records (Receipts, Invoices)",
                    "Correspondence (Emails, Texts)",
                    "Photographs of damaged property",
                    "Witnesses"
                )
            ),
            // Eviction (Rule for Possession)
            Allegation(
                id = "eviction",
                text = "Eviction (Rule for Possession)",
                description = "A legal process through which a landlord can reclaim possession of a rental property from a tenant.",
                elements = listOf(
                    AllegationElement("Landlord-Tenant Relationship", "A valid lease or rental agreement exists."),
                    AllegationElement("Grounds for Eviction", "A lawful reason to evict (e.g., non-payment of rent)."),
                    AllegationElement("Proper Notice to Vacate", "The landlord delivered a written 'Notice to Vacate' to the tenant.")
                ),
                evidenceSuggestions = listOf(
                    "The Lease Agreement",
                    "Copy of the Notice to Vacate",
                    "Proof of Delivery of Notice",
                    "Rent Payment Records",
                    "Photographs or Videos of property damage",
                    "Correspondence with tenant"
                )
            ),
            // Theft
            Allegation(
                id = "theft",
                text = "Theft",
                description = "The taking or misappropriation of anything of value that belongs to another person, without consent or by fraudulent means, with the intent to permanently deprive the owner of their property.",
                elements = listOf(
                    AllegationElement("A Misappropriation or Taking", "The offender exercised wrongful control over the property."),
                    AllegationElement("A Thing of Value", "The object of the theft must have some value."),
                    AllegationElement("Belonging to Another", "The property was owned by someone other than the offender."),
                    AllegationElement("Without Consent or by Fraudulent Means", "The taking was done without permission or through deceit."),
                    AllegationElement("Intent to Permanently Deprive", "The offender meant to keep the item from the owner for good.")
                ),
                evidenceSuggestions = listOf(
                    "Physical Evidence (stolen property)",
                    "Surveillance Footage",
                    "Witness Testimony",
                    "Documentary Evidence (receipts, appraisals)",
                    "Confession or Admission",
                    "Circumstantial Evidence (e.g., possession of stolen goods)"
                )
            ),
            // Assault (Simple & Aggravated)
            Allegation(
                id = "assault",
                text = "Assault (Simple & Aggravated)",
                description = "The threat or attempt to cause physical harm. Aggravated assault involves the use of a dangerous weapon.",
                elements = listOf(
                    AllegationElement("Attempted Battery OR Intentional Apprehension", "The offender either attempted to cause harm or intentionally placed the victim in reasonable fear of harm."),
                    AllegationElement("With a Dangerous Weapon (for Aggravated Assault)", "The assault was committed using a dangerous weapon.")
                ),
                evidenceSuggestions = listOf(
                    "Victim and Witness Testimony",
                    "The Weapon (for Aggravated Assault)",
                    "Photographs or Videos of the incident",
                    "Threatening Communications (voicemails, texts)",
                    "911 Recordings"
                )
            ),
            // Federal Civil Rights Violation (42 U.S.C. § 1983)
            Allegation(
                id = "civil_rights_1983",
                text = "Federal Civil Rights Violation (42 U.S.C. § 1983)",
                description = "A lawsuit against a state or local government official who, while acting in their official capacity, violated a citizen's rights as secured by the U.S. Constitution or federal law.",
                elements = listOf(
                    AllegationElement("Acting Under Color of State Law", "The defendant was a government employee performing their job-related duties."),
                    AllegationElement("Deprivation of Federal Rights", "The defendant's actions violated a right guaranteed by the U.S. Constitution or a federal statute.")
                ),
                evidenceSuggestions = listOf(
                    "Police Reports and Internal Affairs Files",
                    "Body Camera or Dashboard Camera Footage",
                    "Medical Records",
                    "Witness Testimony",
                    "Photographs and Videos of injuries/scene",
                    "Expert Testimony (on police practices)"
                )
            )
        )
    }
}

data class AllegationElement(
    val name: String,
    val description: String
)