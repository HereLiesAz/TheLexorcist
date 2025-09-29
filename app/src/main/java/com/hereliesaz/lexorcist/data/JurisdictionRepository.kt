package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.Court
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JurisdictionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getJurisdictions(): List<Court> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
                if (!file.exists()) {
                    return@withContext emptyList()
                }

                val workbook = WorkbookFactory.create(file)
                val sheet = workbook.getSheet("Jurisdictions")
                val jurisdictions = mutableListOf<Court>()

                val headerRow = sheet.getRow(0)
                val headers = headerRow.cellIterator().asSequence().map { it.stringCellValue }.toList()
                val colIndices = headers.withIndex().associate { (index, name) -> name to index }

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val id = row.getCell(colIndices.getValue("id")).stringCellValue
                    val courtName = row.getCell(colIndices.getValue("courtName")).stringCellValue
                    jurisdictions.add(Court(id = id, courtName = courtName))
                }
                workbook.close()
                jurisdictions
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}