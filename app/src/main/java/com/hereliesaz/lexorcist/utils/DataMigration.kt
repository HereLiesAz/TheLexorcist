package com.hereliesaz.lexorcist.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.model.Court
// Unused import: com.hereliesaz.lexorcist.model.Script
// Unused import: com.hereliesaz.lexorcist.model.Template
// import org.apache.poi.ss.usermodel.WorkbookFactory // This import is unused
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

// Define data classes to match the structure of default_extras.json
data class ScriptFromFile(
    val id: String? = null, // Added id based on TemplateFromFile, though not used in current sheet
    val name: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val description: String? = null,
    val content: String? = null,
    val court: String? = null // Added court based on TemplateFromFile, though not used in current script sheet
)

data class TemplateFromFile(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val content: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val court: String? = null
)

data class ExtrasFromFile(
    val scripts: List<ScriptFromFile>? = null,
    val templates: List<TemplateFromFile>? = null
)

class DataMigration(private val context: Context) {

    private val gson = Gson()
    private val TAG = "DataMigration"

    fun migrate() {
        val workbook = XSSFWorkbook()

        // 1. Migrate allegations_catalog.json
        try {
            val allegationsSheet = workbook.createSheet("Allegations")
            val allegationsJson = context.assets.open("allegations_catalog.json").bufferedReader().use { it.readText() }
            val allegationListType = object : TypeToken<List<MasterAllegation>>() {}.type
            // Ensure MasterAllegation has relevantEvidence field or handle its absence if it's from a different structure
            val allegations: List<MasterAllegation> = gson.fromJson(allegationsJson, allegationListType)

            val allegationHeader = allegationsSheet.createRow(0)
            allegationHeader.createCell(0).setCellValue("id")
            allegationHeader.createCell(1).setCellValue("name")
            allegationHeader.createCell(2).setCellValue("description")
            // allegationHeader.createCell(3).setCellValue("elements") // elements is not in MasterAllegation from previous context
            allegationHeader.createCell(3).setCellValue("category")
            allegationHeader.createCell(4).setCellValue("type")
            allegationHeader.createCell(5).setCellValue("courtLevel")
            // allegationHeader.createCell(7).setCellValue("relevantEvidence") // relevantEvidence is not in MasterAllegation from previous context

            allegations.forEachIndexed { index, allegation ->
                val row = allegationsSheet.createRow(index + 1)
                row.createCell(0).setCellValue(allegation.id)
                row.createCell(1).setCellValue(allegation.name)
                row.createCell(2).setCellValue(allegation.description)
                // row.createCell(3).setCellValue(gson.toJson(allegation.elements)) // elements is not in MasterAllegation
                row.createCell(3).setCellValue(allegation.category)
                row.createCell(4).setCellValue(allegation.type)
                row.createCell(5).setCellValue(allegation.courtLevel)
                // row.createCell(7).setCellValue(gson.toJson(allegation.relevantEvidence)) // relevantEvidence is not in MasterAllegation
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating allegations_catalog.json", e)
        }

        // 2. Migrate exhibits_catalog.json
        try {
            val exhibitsSheet = workbook.createSheet("Exhibits")
            val exhibitsJson = context.assets.open("exhibits_catalog.json").bufferedReader().use { it.readText() }
            val exhibitListType = object : TypeToken<List<ExhibitCatalogItem>>() {}.type
            val exhibits: List<ExhibitCatalogItem> = gson.fromJson(exhibitsJson, exhibitListType)

            val exhibitHeader = exhibitsSheet.createRow(0)
            exhibitHeader.createCell(0).setCellValue("id")
            exhibitHeader.createCell(1).setCellValue("exhibit_type") // Field in ExhibitCatalogItem is 'type'
            exhibitHeader.createCell(2).setCellValue("description")
            exhibitHeader.createCell(3).setCellValue("applicable_allegation_ids")

            exhibits.forEachIndexed { index, exhibit ->
                val row = exhibitsSheet.createRow(index + 1)
                row.createCell(0).setCellValue(exhibit.id)
                row.createCell(1).setCellValue(exhibit.type) // Changed from exhibit.exhibit_type
                row.createCell(2).setCellValue(exhibit.description)
                row.createCell(3).setCellValue(exhibit.applicableAllegationIds.joinToString(","))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating exhibits_catalog.json", e)
        }

        // 3. Migrate jurisdictions.json
        try {
            val jurisdictionsSheet = workbook.createSheet("Jurisdictions")
            val jurisdictionsJson = context.assets.open("jurisdictions.json").bufferedReader().use { it.readText() }
            val jurisdictionListType = object : TypeToken<List<Court>>() {}.type
            val jurisdictions: List<Court> = gson.fromJson(jurisdictionsJson, jurisdictionListType)

            val jurisdictionHeader = jurisdictionsSheet.createRow(0)
            jurisdictionHeader.createCell(0).setCellValue("id")
            jurisdictionHeader.createCell(1).setCellValue("courtName")

            jurisdictions.forEachIndexed { index, jurisdiction ->
                val row = jurisdictionsSheet.createRow(index + 1)
                row.createCell(0).setCellValue(jurisdiction.id)
                row.createCell(1).setCellValue(jurisdiction.courtName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating jurisdictions.json", e)
        }

        // 4. Migrate default_extras.json
        try {
            val extrasJson = context.assets.open("default_extras.json").bufferedReader().use { it.readText() }
            val extrasType = object : TypeToken<ExtrasFromFile>() {}.type
            val extras: ExtrasFromFile? = gson.fromJson(extrasJson, extrasType)

            if (extras != null) {
                // Scripts
                extras.scripts?.let {
                    val scriptsSheet = workbook.createSheet("DefaultScripts")
                    val scriptHeader = scriptsSheet.createRow(0)
                    scriptHeader.createCell(0).setCellValue("name")
                    scriptHeader.createCell(1).setCellValue("authorName")
                    scriptHeader.createCell(2).setCellValue("authorEmail")
                    scriptHeader.createCell(3).setCellValue("description")
                    scriptHeader.createCell(4).setCellValue("content")
                    // scriptHeader.createCell(5).setCellValue("id") // No 'id' column in original code for scripts
                    // scriptHeader.createCell(6).setCellValue("court") // No 'court' column in original code for scripts

                    it.forEachIndexed { index, script ->
                        val row = scriptsSheet.createRow(index + 1)
                        row.createCell(0).setCellValue(script.name)
                        row.createCell(1).setCellValue(script.authorName)
                        row.createCell(2).setCellValue(script.authorEmail)
                        row.createCell(3).setCellValue(script.description)
                        row.createCell(4).setCellValue(script.content)
                        // row.createCell(5).setCellValue(script.id)  // No 'id' column in original code for scripts
                        // row.createCell(6).setCellValue(script.court) // No 'court' column in original code for scripts
                    }
                }

                // Templates
                extras.templates?.let {
                    val templatesSheet = workbook.createSheet("DefaultTemplates")
                    val templateHeader = templatesSheet.createRow(0)
                    templateHeader.createCell(0).setCellValue("id")
                    templateHeader.createCell(1).setCellValue("name")
                    templateHeader.createCell(2).setCellValue("description")
                    templateHeader.createCell(3).setCellValue("content")
                    templateHeader.createCell(4).setCellValue("authorName")
                    templateHeader.createCell(5).setCellValue("authorEmail")
                    templateHeader.createCell(6).setCellValue("court")

                    it.forEachIndexed { index, template ->
                        val row = templatesSheet.createRow(index + 1)
                        row.createCell(0).setCellValue(template.id)
                        row.createCell(1).setCellValue(template.name)
                        row.createCell(2).setCellValue(template.description)
                        row.createCell(3).setCellValue(template.content)
                        row.createCell(4).setCellValue(template.authorName)
                        row.createCell(5).setCellValue(template.authorEmail)
                        row.createCell(6).setCellValue(template.court)
                    }
                }
            } else {
                Log.w(TAG, "default_extras.json was parsed as null or is empty.")
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "default_extras.json not found in assets. Skipping migration for default extras.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating default_extras.json", e)
        }

        // 5. Migrate statistical_weights.json
        try {
            val weightsSheet = workbook.createSheet("StatisticalWeights")
            val weightsJson = context.assets.open("statistical_weights.json").bufferedReader().use { it.readText() }
            val weightsHeader = weightsSheet.createRow(0)
            weightsHeader.createCell(0).setCellValue("data")
            val weightsDataRow = weightsSheet.createRow(1)
            weightsDataRow.createCell(0).setCellValue(weightsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating statistical_weights.json", e)
        }

        // Write the workbook to a file
        try {
            val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
            FileOutputStream(file).use {
                workbook.write(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing lexorcist_catalogs.xlsx", e)
        } finally {
            try {
                workbook.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing workbook", e)
            }
        }
    }
}