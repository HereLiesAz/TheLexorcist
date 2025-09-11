package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterAllegationRepositoryImpl @Inject constructor() : MasterAllegationRepository {

    private val masterAllegations = listOf(
        // Civil Allegations
        MasterAllegation("Civil", "Personal injury (Torts)", "Negligence", "Failure to exercise reasonable care, e.g., car accidents, slip-and-fall, medical malpractice, wrongful death."),
        MasterAllegation("Civil", "Personal injury (Torts)", "Assault and battery", "Intentional act causing fear of attack (assault) or unlawful physical contact (battery)."),
        MasterAllegation("Civil", "Personal injury (Torts)", "False imprisonment", "Unlawful confinement of a person without their consent."),
        MasterAllegation("Civil", "Personal injury (Torts)", "Intentional infliction of emotional distress", "Extreme or outrageous conduct that causes severe emotional harm."),
        MasterAllegation("Civil", "Personal injury (Torts)", "Trespass", "Intentional entry onto the land of another without permission."),
        MasterAllegation("Civil", "Personal injury (Torts)", "Product Liability", "Holds manufacturers, distributors, or sellers responsible for a defective product that causes harm."),
        MasterAllegation("Civil", "Contract disputes", "Breach of contract", "Failure to perform the duties stipulated in a valid and binding contract."),
        MasterAllegation("Civil", "Contract disputes", "Fraudulent inducement", "Alleging that a party was intentionally deceived into entering a contract."),
        MasterAllegation("Civil", "Contract disputes", "Duress", "Claiming a party was coerced into an agreement."),
        MasterAllegation("Civil", "Contract disputes", "Lesion", "An allegation where a contract's price is unfairly low, typically for immovable property."),
        MasterAllegation("Civil", "Contract disputes", "Specific performance", "Seeking a court order for the other party to fulfill their part of the contract, rather than paying monetary damages."),
        MasterAllegation("Civil", "Property disputes", "Boundary disputes", "When neighbors disagree on the exact location of their property lines."),
        MasterAllegation("Civil", "Property disputes", "Servitude disputes", "Disagreements over the right to use another person's property for a specific purpose, similar to an easement in other states."),
        MasterAllegation("Civil", "Property disputes", "Acquisitive prescription (Adverse Possession)", "A claim to ownership of property by someone who has openly occupied it for an extended period."),
        MasterAllegation("Civil", "Property disputes", "Landlord-Tenant disputes", "Allegations concerning rent, property maintenance, or eviction."),
        MasterAllegation("Civil", "Property disputes", "Co-ownership disputes", "Disagreements among multiple owners of a single property."),
        MasterAllegation("Civil", "Family law", "Divorce", "Allegations related to fault or no-fault grounds for ending a marriage."),
        MasterAllegation("Civil", "Family law", "Custody proceedings", "Actions to determine child custody and support."),
        MasterAllegation("Civil", "Family law", "Filiation actions", "Claims to establish or disavow the legal relationship between a child and a parent."),
        MasterAllegation("Civil", "Family law", "Child abuse and neglect", "Legal services are mandated in cases of alleged abuse, neglect, or exploitation."),
        MasterAllegation("Civil", "Family law", "Protective orders", "Allegations involving domestic violence that require a protective order."),
        MasterAllegation("Civil", "Civil rights violations", "Discrimination", "Claims of discrimination based on a protected characteristic in employment, housing, or public accommodation."),
        MasterAllegation("Civil", "Civil rights violations", "Violations under 42 U.S.C. § 1983", "This covers constitutional violations by state actors, such as allegations of excessive force by law enforcement."),
        MasterAllegation("Civil", "Other civil allegations", "Open account or promissory note actions", "Claims to collect on unpaid commercial accounts or promissory notes."),
        MasterAllegation("Civil", "Other civil allegations", "Insurance policy disputes", "Disputes over claims against an insurance company."),
        MasterAllegation("Civil", "Other civil allegations", "Suits on a judicial bond", "Actions to recover damages from a bond."),

        // Criminal Allegations
        MasterAllegation("Criminal", "Offenses Against Persons", "Homicide", "First degree murder, second degree murder, manslaughter, negligent homicide, vehicular homicide."),
        MasterAllegation("Criminal", "Offenses Against Persons", "Feticide", "First degree feticide, second degree feticide, third degree feticide."),
        MasterAllegation("Criminal", "Offenses Against Persons", "Assault and Battery", "Aggravated assault, simple assault, aggravated battery, second degree battery, simple battery."),
        MasterAllegation("Criminal", "Offenses Against Persons", "Rape and Sexual Offenses", "Forcible rape, simple rape, sexual battery, oral sexual battery, etc."),
        MasterAllegation("Criminal", "Offenses Against Persons", "Kidnapping", "Aggravated kidnapping, second degree kidnapping, simple kidnapping."),
        MasterAllegation("Criminal", "Offenses Against Persons", "Robbery", "First degree robbery, second degree robbery, simple robbery, carjacking."),
        MasterAllegation("Criminal", "Offenses Affecting Property", "Arson and Use of Explosives", "Aggravated arson, simple arson."),
        MasterAllegation("Criminal", "Offenses Affecting Property", "Burglary", "Aggravated burglary, simple burglary."),
        MasterAllegation("Criminal", "Offenses Affecting Property", "Theft", "Theft, theft of a firearm, theft of livestock, etc."),
        MasterAllegation("Criminal", "Offenses Affecting Property", "Fraud and Forgery", "Issuing worthless checks, forgery, identity theft, etc."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Morals", "Gambling", "Gambling, gambling in public, etc."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Morals", "Obscenity", "Obscenity, pornography involving juveniles."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Morals", "Prostitution", "Prostitution, soliciting for prostitutes, etc."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Generally", "Bribery and Corrupt Influencing", "Public bribery, bribery of sports participants, etc."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Generally", "Treason and Disloyal Acts", "Treason, misprision of treason."),
        MasterAllegation("Criminal", "Offenses Affecting the Public Generally", "Weapons", "Illegal carrying of weapons, illegal use of weapons or dangerous instrumentalities.")
    )

    override fun getMasterAllegations(): Flow<List<MasterAllegation>> {
        return flowOf(masterAllegations)
    }
}
