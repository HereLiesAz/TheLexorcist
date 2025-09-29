package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// A sealed interface to represent a shared item, which can be a Script or a Template.
sealed interface SharedItem {
    val id: String
    val name: String
    val description: String
    val authorName: String
    val authorEmail: String
    val content: String
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
    private val googleApiService: GoogleApiService,
    @ApplicationContext private val context: Context
) {

    suspend fun getSharedItems(): Result<List<SharedItem>> {
        return try {
            val scripts = googleApiService.getSharedScripts().map { ScriptItem(it) }
            val templates = googleApiService.getSharedTemplates().map { TemplateItem(it) }
            Result.Success(scripts + templates)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getDefaultScripts(): List<Script> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
                if (!file.exists()) return@withContext emptyList()

                val workbook = WorkbookFactory.create(file)
                val sheet = workbook.getSheet("DefaultScripts")
                val scripts = mutableListOf<Script>()
                val headerRow = sheet.getRow(0)
                val headers = headerRow.cellIterator().asSequence().map { it.stringCellValue }.toList()
                val colIndices = headers.withIndex().associate { (index, name) -> name to index }

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    scripts.add(
                        Script(
                            id = "default_script_$i", // Generating a unique ID
                            name = row.getCell(colIndices.getValue("name")).stringCellValue,
                            authorName = row.getCell(colIndices.getValue("authorName")).stringCellValue,
                            authorEmail = row.getCell(colIndices.getValue("authorEmail")).stringCellValue,
                            description = row.getCell(colIndices.getValue("description")).stringCellValue,
                            content = row.getCell(colIndices.getValue("content")).stringCellValue
                        )
                    )
                }
                workbook.close()
                scripts
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getDefaultTemplates(): List<Template> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
                if (!file.exists()) return@withContext emptyList()

                val workbook = WorkbookFactory.create(file)
                val sheet = workbook.getSheet("DefaultTemplates")
                val templates = mutableListOf<Template>()
                val headerRow = sheet.getRow(0)
                val headers = headerRow.cellIterator().asSequence().map { it.stringCellValue }.toList()
                val colIndices = headers.withIndex().associate { (index, name) -> name to index }

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    templates.add(
                        Template(
                            id = row.getCell(colIndices.getValue("id")).stringCellValue,
                            name = row.getCell(colIndices.getValue("name")).stringCellValue,
                            description = row.getCell(colIndices.getValue("description")).stringCellValue,
                            content = row.getCell(colIndices.getValue("content")).stringCellValue,
                            authorName = row.getCell(colIndices.getValue("authorName")).stringCellValue,
                            authorEmail = row.getCell(colIndices.getValue("authorEmail")).stringCellValue,
                            court = row.getCell(colIndices.getValue("court"))?.stringCellValue
                        )
                    )
                }
                workbook.close()
                templates
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun deleteSharedItem(item: SharedItem, userEmail: String): Result<Unit> {
        val originalItem = when (item) {
            is ScriptItem -> item.script
            is TemplateItem -> item.template
        }
        return googleApiService.deleteSharedItem(originalItem, userEmail)
    }

    suspend fun updateSharedItem(item: SharedItem, userEmail: String): Result<Unit> {
        val originalItem = when (item) {
            is ScriptItem -> item.script
            is TemplateItem -> item.template
        }
        return googleApiService.updateSharedItem(originalItem, userEmail)
    }

    suspend fun shareItem(
        name: String,
        description: String,
        content: String,
        type: String,
        authorName: String,
        authorEmail: String,
        court: String?
    ): Result<Unit> {
        return googleApiService.shareAddon(name, description, content, type, authorName, authorEmail, court ?: "")
    }

    suspend fun rateAddon(id: String, rating: Int, type: String): Boolean {
        return googleApiService.rateAddon(id, rating, type)
    }
}