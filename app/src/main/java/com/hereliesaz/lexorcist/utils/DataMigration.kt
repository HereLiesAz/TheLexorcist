package com.hereliesaz.lexorcist.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.model.Court
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class DataMigration(private val context: Context) {

    private val gson = Gson()

    fun migrate() {
        val workbook = XSSFWorkbook()

        // 1. Migrate allegations_catalog.json
        val allegationsSheet = workbook.createSheet("Allegations")
        val allegationsJson = context.assets.open("allegations_catalog.json").bufferedReader().use { it.readText() }
        val allegationListType = object : TypeToken<List<MasterAllegation>>() {}.type
        val allegations: List<MasterAllegation> = gson.fromJson(allegationsJson, allegationListType)

        val allegationHeader = allegationsSheet.createRow(0)
        allegationHeader.createCell(0).setCellValue("id")
        allegationHeader.createCell(1).setCellValue("name")
        allegationHeader.createCell(2).setCellValue("description")
        allegationHeader.createCell(3).setCellValue("elements")
        allegationHeader.createCell(4).setCellValue("category")
        allegationHeader.createCell(5).setCellValue("type")
        allegationHeader.createCell(6).setCellValue("courtLevel")
        allegationHeader.createCell(7).setCellValue("relevantEvidence")

        allegations.forEachIndexed { index, allegation ->
            val row = allegationsSheet.createRow(index + 1)
            row.createCell(0).setCellValue(allegation.id)
            row.createCell(1).setCellValue(allegation.name)
            row.createCell(2).setCellValue(allegation.description)
            row.createCell(3).setCellValue(gson.toJson(allegation.elements))
            row.createCell(4).setCellValue(allegation.category)
            row.createCell(5).setCellValue(allegation.type)
            row.createCell(6).setCellValue(allegation.courtLevel)
            row.createCell(7).setCellValue(gson.toJson(allegation.relevantEvidence))
        }

        // 2. Migrate exhibits_catalog.json
        val exhibitsSheet = workbook.createSheet("Exhibits")
        val exhibitsJson = context.assets.open("exhibits_catalog.json").bufferedReader().use { it.readText() }
        val exhibitListType = object : TypeToken<List<ExhibitCatalogItem>>() {}.type
        val exhibits: List<ExhibitCatalogItem> = gson.fromJson(exhibitsJson, exhibitListType)

        val exhibitHeader = exhibitsSheet.createRow(0)
        exhibitHeader.createCell(0).setCellValue("id")
        exhibitHeader.createCell(1).setCellValue("exhibit_type")
        exhibitHeader.createCell(2).setCellValue("description")
        exhibitHeader.createCell(3).setCellValue("applicable_allegation_ids")

        exhibits.forEachIndexed { index, exhibit ->
            val row = exhibitsSheet.createRow(index + 1)
            row.createCell(0).setCellValue(exhibit.id)
            row.createCell(1).setCellValue(exhibit.type)
            row.createCell(2).setCellValue(exhibit.description)
            row.createCell(3).setCellValue(exhibit.applicableAllegationIds.joinToString(","))
        }

        // 3. Migrate jurisdictions.json
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

        // 4. Migrate default_extras.json
        val extrasJson = context.assets.open("default_extras.json").bufferedReader().use { it.readText() }
        val extrasType = object : TypeToken<Map<String, Any>>() {}.type
        val extras: Map<String, Any> = gson.fromJson(extrasJson, extrasType)

        // Scripts
        val scriptsSheet = workbook.createSheet("DefaultScripts")
        val scriptHeader = scriptsSheet.createRow(0)
        scriptHeader.createCell(0).setCellValue("name")
        scriptHeader.createCell(1).setCellValue("authorName")
        scriptHeader.createCell(2).setCellValue("authorEmail")
        scriptHeader.createCell(3).setCellValue("description")
        scriptHeader.createCell(4).setCellValue("content")

        val scriptsList = extras["scripts"] as List<Map<String, String>>
        scriptsList.forEachIndexed { index, scriptMap ->
            val row = scriptsSheet.createRow(index + 1)
            row.createCell(0).setCellValue(scriptMap["name"])
            row.createCell(1).setCellValue(scriptMap["authorName"])
            row.createCell(2).setCellValue(scriptMap["authorEmail"])
            row.createCell(3).setCellValue(scriptMap["description"])
            row.createCell(4).setCellValue(scriptMap["content"])
        }

        // Templates
        val templatesSheet = workbook.createSheet("DefaultTemplates")
        val templateHeader = templatesSheet.createRow(0)
        templateHeader.createCell(0).setCellValue("id")
        templateHeader.createCell(1).setCellValue("name")
        templateHeader.createCell(2).setCellValue("description")
        templateHeader.createCell(3).setCellValue("content")
        templateHeader.createCell(4).setCellValue("authorName")
        templateHeader.createCell(5).setCellValue("authorEmail")
        templateHeader.createCell(6).setCellValue("court")

        val templatesList = extras["templates"] as List<Map<String, String>>
        templatesList.forEachIndexed { index, templateMap ->
            val row = templatesSheet.createRow(index + 1)
            row.createCell(0).setCellValue(templateMap["id"])
            row.createCell(1).setCellValue(templateMap["name"])
            row.createCell(2).setCellValue(templateMap["description"])
            row.createCell(3).setCellValue(templateMap["content"])
            row.createCell(4).setCellValue(templateMap["authorName"])
            row.createCell(5).setCellValue(templateMap["authorEmail"])
            row.createCell(6).setCellValue(templateMap["court"])
        }

        // 5. Migrate statistical_weights.json
        val weightsSheet = workbook.createSheet("StatisticalWeights")
        val weightsJson = context.assets.open("statistical_weights.json").bufferedReader().use { it.readText() }
        val weightsHeader = weightsSheet.createRow(0)
        weightsHeader.createCell(0).setCellValue("data")
        val weightsDataRow = weightsSheet.createRow(1)
        weightsDataRow.createCell(0).setCellValue(weightsJson)


        // Write the workbook to a file
        val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
        FileOutputStream(file).use {
            workbook.write(it)
        }
        workbook.close()
    }
}