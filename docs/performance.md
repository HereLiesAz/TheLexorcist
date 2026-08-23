# Performance

This file used to contain no application performance guidance at all. It was a
build budget for contributors:

> **Minimize Builds:** Do not run a build or a test without explicit
> instruction... An ideal workflow requires **at most one build, if any at
> all.**
>
> **Confidence in Code:** Rely on thorough analysis and code quality to avoid
> the need for frequent builds.

Together with `docs/testing.md`'s "Do Not Test Unprompted", that is how a
version catalog that does not parse reached `main` and stayed there. Both rules
are gone; see `docs/testing.md` for what replaced them.

What follows is about the application.

## Known hot spots

### The `.xlsx` database is O(whole database) per write

`LocalFileStorageService` is the single source of truth, and every mutation --
adding one allegation, toggling one exhibit, editing one transcript -- parses
the entire workbook into memory with Apache POI, changes a cell, and writes the
whole file back out. All cases live in one `lexorcist_data.xlsx`, so the cost of
any edit scales with the size of the user's whole case load, not with the size
of the edit.

`XSSFWorkbook` holds the full document object model in memory. A case with
thousands of evidence rows is a real out-of-memory risk on a low-end device.

Writes are now serialised behind a `Mutex` and go through a temp-file-and-rename
so they cannot corrupt the database, which is correct but does not make them
cheaper. The structural fix is a real embedded database with row-level writes
and an `.xlsx` *export*, rather than a spreadsheet used as the storage engine.
Portability was the reason for the spreadsheet, and an export button delivers
that without giving up transactions, indexes and migrations.

### `EVIDENCE_HEADER.indexOf(...)` in row loops

The read and write paths call `indexOf` on the header list once per column per
row -- a linear scan of a 19-element list, nineteen times a row. Resolve the
indices once before the loop.

### Scripts re-run on screen load

`docs/workflow.md` describes running all active scripts over all evidence on
every screen load, gated on a modification check. The gate is
`"$evidenceId:$scriptId"` in DataStore, which records nothing about the script's
*content*, so an edited script does not invalidate it. That is a correctness
problem before it is a performance one; see the scripting notes in `TODO.md`.

### Video frame OCR

`VideoProcessingService` extracts a frame every 5 seconds and runs ML Kit over
each one, accumulating into a single `StringBuilder` that is only returned once
the whole video is processed. Nothing is persisted incrementally, so a decoder
failure at minute 9 of a 10-minute video discards all prior work.

### Apache POI keeps the APK large

`proguard-rules.pro` contains `-keep class org.apache.poi.** { *; }`, which
makes the largest dependency in the app entirely unshrinkable even though
`isMinifyEnabled = true`.

## Measuring

Prefer a measurement to a guess:

- `./gradlew :app:assembleRelease` then check the APK with the AGP APK
  Analyzer for size regressions.
- Macrobenchmark or systrace for startup; note that
  `LocalFileStorageService.init` calls `initializeSpreadsheet()` in the
  constructor of a `@Singleton`, so workbook I/O happens on whichever thread
  Hilt first injects it.
- StrictMode in debug builds catches disk and network access on the main
  thread.
