package com.hereliesaz.lexorcist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a script shared in the "Extras" ecosystem (Marketplace).
 *
 * This class is distinct from `data.Script`, which represents a local script entity.
 * `model.Script` includes additional metadata required for the sharing and rating system,
 * such as author information, ratings, and court jurisdiction.
 */
@Parcelize
data class Script(
    /** Unique identifier for the script (e.g., UUID or Spreadsheet Row ID). */
    val id: String,
    /** The display name of the script. */
    val name: String,
    /** A detailed description of what the script does. */
    val description: String,
    /** The actual JavaScript code content. */
    val content: String,
    /** The name of the author (manually entered). */
    val authorName: String = "",
    /** The email of the author (used for ownership verification in the Extras sheet). */
    val authorEmail: String = "",
    /** The standardized court position or jurisdiction this script applies to (optional). */
    val court: String? = null,
    /** The average rating of the script (0.0 to 5.0). */
    val rating: Double = 0.0,
    /** The total number of ratings this script has received. */
    val numRatings: Int = 0
) : Parcelable
