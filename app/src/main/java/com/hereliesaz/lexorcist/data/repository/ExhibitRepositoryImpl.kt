package com.hereliesaz.lexorcist.data.repository

import android.content.Context
import com.google.gson.Gson
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
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
class ExhibitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : ExhibitRepository {

    override fun getExhibitCatalog(): Flow<List<ExhibitCatalogItem>> = flow {
        try {
            val file = File(context.filesDir, "lexorcist_catalogs.xlsx")
            if (!file.exists()) {
                emit(emptyList())
                return@flow
            }

            val workbook = WorkbookFactory.create(file)
            val sheet = workbook.getSheet("Exhibits")
            val exhibits = mutableListOf<ExhibitCatalogItem>()

            val headerRow = sheet.getRow(0)
            val headers = headerRow.cellIterator().asSequence().map { it.stringCellValue }.toList()
            val colIndices = headers.withIndex().associate { (index, name) -> name to index }

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                val id = row.getCell(colIndices.getValue("id")).stringCellValue
                val type = row.getCell(colIndices.getValue("exhibit_type")).stringCellValue
                val description = row.getCell(colIndices.getValue("description")).stringCellValue
                val applicableAllegationIdsStr = row.getCell(colIndices.getValue("applicable_allegation_ids"))?.stringCellValue ?: ""
                val applicableAllegationIds = if (applicableAllegationIdsStr.isNotBlank()) {
                    applicableAllegationIdsStr.split(",")
                } else {
                    emptyList()
                }

                exhibits.add(
                    ExhibitCatalogItem(
                        id = id,
                        type = type,
                        description = description,
                        applicableAllegationIds = applicableAllegationIds
                    )
                )
            }
            workbook.close()
            emit(exhibits)

        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}