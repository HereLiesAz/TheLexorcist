package com.hereliesaz.lexorcist.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.RelevantEvidence
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val gson = Gson()

    /**
     * Loads the master list of allegations from the "Allegations" sheet
     * in the `lexorcist_catalogs.xlsx` file.
     */
    fun getMasterAllegations(): Flow<List<MasterAllegation>> = flow {
        try {
            val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
            if (!file.exists()) {
                // In a real app, you might trigger the migration here if needed,
                // but for now, we assume it has run.
                emit(emptyList())
                return@flow
            }

            val workbook = WorkbookFactory.create(file)
            val sheet = workbook.getSheet("Allegations")
            val allegations = mutableListOf<MasterAllegation>()

            // Get header row to map column names to indices
            val headerRow = sheet.getRow(0)
            val headers = headerRow.cellIterator().asSequence().map { it.stringCellValue }.toList()
            val colIndices = headers.withIndex().associate { (index, name) -> name to index }

            // Iterate over data rows
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                val id = row.getCell(colIndices.getValue("id")).stringCellValue
                val name = row.getCell(colIndices.getValue("name")).stringCellValue
                val description = row.getCell(colIndices.getValue("description"))?.stringCellValue
                val category = row.getCell(colIndices.getValue("category"))?.stringCellValue
                val type = row.getCell(colIndices.getValue("type"))?.stringCellValue
                val courtLevel = row.getCell(colIndices.getValue("courtLevel"))?.stringCellValue

                val elementsJson = row.getCell(colIndices.getValue("elements"))?.stringCellValue
                val elements: List<String>? = if (elementsJson != null) {
                    gson.fromJson(elementsJson, object : TypeToken<List<String>>() {}.type)
                } else null

                val relevantEvidenceJson = row.getCell(colIndices.getValue("relevantEvidence"))?.stringCellValue
                val relevantEvidence: RelevantEvidence? = if (relevantEvidenceJson != null) {
                    gson.fromJson(relevantEvidenceJson, RelevantEvidence::class.java)
                } else null

                allegations.add(
                    MasterAllegation(
                        id = id,
                        name = name,
                        description = description,
                        category = category,
                        type = type,
                        courtLevel = courtLevel,
                        elements = elements,
                        relevantEvidence = relevantEvidence
                    )
                )
            }
            workbook.close()
            emit(allegations)

        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}