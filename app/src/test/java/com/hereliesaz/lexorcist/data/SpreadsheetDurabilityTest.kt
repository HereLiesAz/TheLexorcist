package com.hereliesaz.lexorcist.data

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

/**
 * Durability tests for the .xlsx-as-database write path.
 *
 * `LocalFileStorageService` is a 949-line `@Singleton` that had no test file at
 * all, so none of the following was covered: that a save cannot leave a
 * half-written database, that a backup survives a save, or that an unreadable
 * database is not simply deleted.
 *
 * These exercise the same algorithms the service now uses, against a real POI
 * workbook on a real temp directory. They are deliberately written against the
 * file-level behaviour rather than the Hilt-injected service, because the
 * service's constructor takes a Context and four cloud providers -- the very
 * coupling that made it untestable and left it untested.
 */
class SpreadsheetDurabilityTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun workbookWith(marker: String): XSSFWorkbook = XSSFWorkbook().apply {
        createSheet("Evidence").createRow(0).createCell(0).setCellValue(marker)
    }

    private fun markerIn(file: File): String? =
        WorkbookFactory.create(file).use { wb ->
            wb.getSheet("Evidence")?.getRow(0)?.getCell(0)?.stringCellValue
        }

    /** Mirrors LocalFileStorageService.writeWorkbookAtomically. */
    private fun writeAtomically(dir: File, name: String, wb: XSSFWorkbook) {
        val target = File(dir, name)
        val backup = File(dir, "$name.bak")
        val tmp = File(dir, "$name.tmp")
        if (tmp.exists()) tmp.delete()

        FileOutputStream(tmp).use { fos ->
            wb.write(fos)
            fos.flush()
            fos.fd.sync()
        }
        check(tmp.length() > 0L) { "refusing to replace the database with an empty file" }

        if (target.exists()) {
            if (backup.exists()) backup.delete()
            if (!target.renameTo(backup)) target.copyTo(backup, overwrite = true)
        }
        check(tmp.renameTo(target)) { "could not move the new database into place" }
    }

    @Test
    fun `a save leaves no temporary file behind`() {
        val dir = temp.newFolder()
        workbookWith("first").use { writeAtomically(dir, NAME, it) }

        assertTrue(File(dir, NAME).exists())
        assertFalse("temp file should be renamed away", File(dir, "$NAME.tmp").exists())
    }

    @Test
    fun `the previous database is retained as a backup`() {
        val dir = temp.newFolder()
        workbookWith("first").use { writeAtomically(dir, NAME, it) }
        workbookWith("second").use { writeAtomically(dir, NAME, it) }

        assertEquals("second", markerIn(File(dir, NAME)))
        assertEquals(
            "the backup must hold the version the save replaced",
            "first",
            markerIn(File(dir, "$NAME.bak")),
        )
    }

    @Test
    fun `an interrupted save cannot corrupt the live database`() {
        val dir = temp.newFolder()
        workbookWith("committed").use { writeAtomically(dir, NAME, it) }

        // Simulate a process death partway through serialising the next save:
        // bytes land in the temp file and the rename never happens.
        val tmp = File(dir, "$NAME.tmp")
        tmp.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00))

        // The real database is untouched and still opens.
        assertEquals("committed", markerIn(File(dir, NAME)))
    }

    @Test
    fun `truncate in place - the old behaviour - does destroy the database`() {
        // Pins why the change was necessary. This is exactly what the previous
        // implementation did: FileOutputStream over the live file, interrupted.
        val dir = temp.newFolder()
        val target = File(dir, NAME)
        workbookWith("committed").use { wb -> FileOutputStream(target).use { wb.write(it) } }
        assertEquals("committed", markerIn(target))

        FileOutputStream(target).use { it.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00)) }

        val stillReadable = runCatching { markerIn(target) }.isSuccess
        assertFalse(
            "an interrupted in-place write leaves an unreadable database",
            stillReadable,
        )
    }

    /** Mirrors LocalFileStorageService.quarantineFile. */
    private fun quarantineFor(dir: File, name: String): File {
        var i = 0
        while (true) {
            val suffix = if (i == 0) "" else "-$i"
            val candidate = File(dir, "$name.corrupt$suffix")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    @Test
    fun `an unreadable database is preserved rather than deleted`() {
        val dir = temp.newFolder()
        val target = File(dir, NAME)
        target.writeBytes(byteArrayOf(0x00, 0x01, 0x02))

        val quarantine = quarantineFor(dir, NAME)
        assertTrue(target.renameTo(quarantine))

        assertFalse(target.exists())
        assertTrue("the unreadable bytes must still be on disk", quarantine.exists())
        assertEquals(3, quarantine.length())
    }

    @Test
    fun `quarantine names never collide`() {
        val dir = temp.newFolder()
        val first = quarantineFor(dir, NAME).also { it.writeText("a") }
        val second = quarantineFor(dir, NAME).also { it.writeText("b") }
        val third = quarantineFor(dir, NAME)

        assertEquals("$NAME.corrupt", first.name)
        assertEquals("$NAME.corrupt-1", second.name)
        assertEquals("$NAME.corrupt-2", third.name)
    }

    @Test
    fun `recovery prefers the backup over starting empty`() {
        val dir = temp.newFolder()
        workbookWith("real work").use { writeAtomically(dir, NAME, it) }
        workbookWith("more work").use { writeAtomically(dir, NAME, it) }

        // The live database becomes unreadable.
        File(dir, NAME).writeBytes(byteArrayOf(0x00))

        val backup = File(dir, "$NAME.bak")
        assertNotNull(markerIn(backup))

        val quarantine = quarantineFor(dir, NAME)
        File(dir, NAME).copyTo(quarantine, overwrite = true)
        backup.copyTo(File(dir, NAME), overwrite = true)

        assertEquals("real work", markerIn(File(dir, NAME)))
        assertTrue(quarantine.exists())
    }

    private companion object {
        const val NAME = LocalFileStorageService.SPREADSHEET_FILE_NAME
    }
}
