package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

// A sealed interface to represent a shared item, which can be a Script or a Template.
sealed interface SharedItem {
    val id: String
    val name: String
    val description: String
    val authorName: String // Added
    val authorEmail: String // Renamed from author
    val content: String
    // val author: String // REMOVED
    val type: String
    val rating: Double
    val numRatings: Int
    val court: String?

    companion object {
        fun from(script: Script): SharedItem = ScriptItem(script)
        fun from(template: Template): SharedItem = TemplateItem(template)
    }
}

data class ScriptItem(val script: Script) : SharedItem {
    override val id: String get() = script.id
    override val name: String get() = script.name
    override val description: String get() = script.description
    override val authorName: String get() = script.authorName
    override val authorEmail: String get() = script.authorEmail
    override val content: String get() = script.content
    override val type: String = "Script"
    override val rating: Double get() = script.rating
    override val numRatings: Int get() = script.numRatings
    override val court: String? get() = script.court
}

data class TemplateItem(val template: Template) : SharedItem {
    override val id: String get() = template.id
    override val name: String get() = template.name
    override val description: String get() = template.description
    override val authorName: String get() = template.authorName
    override val authorEmail: String get() = template.authorEmail
    override val content: String get() = template.content
    override val type: String = "Template"
    override val rating: Double get() = template.rating
    override val numRatings: Int get() = template.numRatings
    override val court: String? get() = template.court
}

@Singleton
class ExtrasRepository @Inject constructor(
    private val googleApiService: GoogleApiService
) {

    suspend fun getSharedItems(): Result<List<SharedItem>> {
        return try {
            // Assuming googleApiService.getSharedScripts() now returns Scripts with authorName and authorEmail populated
            val scripts = googleApiService.getSharedScripts().map { ScriptItem(it) }
            // Assuming googleApiService.getSharedTemplates() now returns Templates with authorName and authorEmail populated
            val templates = googleApiService.getSharedTemplates().map { TemplateItem(it) }
            Result.Success(scripts + templates)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteSharedItem(item: SharedItem, userEmail: String): Result<Unit> {
        val originalItem = when (item) {
            is ScriptItem -> item.script
            is TemplateItem -> item.template
        }
        // This call will now pass a Script/Template object that has authorEmail correctly set.
        // googleApiService.deleteSharedItem should ideally use originalItem.authorEmail for its checks.
        return googleApiService.deleteSharedItem(originalItem, userEmail)
    }

    suspend fun updateSharedItem(item: SharedItem, userEmail: String): Result<Unit> {
        val originalItem = when (item) {
            is ScriptItem -> item.script
            is TemplateItem -> item.template
        }
        // Similar to delete, googleApiService.updateSharedItem should use originalItem.authorEmail.
        return googleApiService.updateSharedItem(originalItem, userEmail)
    }

    suspend fun shareItem(
        name: String,
        description: String,
        content: String,
        type: String,
        authorName: String, // New parameter
        authorEmail: String,
        court: String?
    ): Result<Unit> {
        // CRITICAL: You MUST update googleApiService.shareAddon to accept authorName.
        return googleApiService.shareAddon(name, description, content, type, authorName, authorEmail, court ?: "")
    }

    suspend fun rateAddon(id: String, rating: Int, type: String): Boolean {
        return googleApiService.rateAddon(id, rating, type)
    }
}
