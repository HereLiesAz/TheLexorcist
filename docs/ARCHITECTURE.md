# Application Architecture

## Modules

```
:shared   Kotlin/Compose Multiplatform. Android + JVM + iOS.
:app      Android application. Platform integrations and the Hilt graph.
```

`:shared` holds everything that does not need a platform: domain models, the
`LexResult` type, pure parsing and analysis, and Compose Multiplatform UI.
`:app` holds everything that does: Google APIs, Apache POI, ML Kit, Rhino,
Vosk, WorkManager, Activity and the Hilt dependency graph.

Apple targets in `:shared` are declared only when the build runs on a macOS
host, so Linux CI stays resolvable. Nothing in `commonMain` depends on that
being true — the split is a build-host concern, not a code one.

Hilt lives in `:app` only. It is an Android-specific, KSP-based framework, so
it cannot be used from common code. `:shared` uses plain constructor injection
and interfaces; `:app` binds them.

## The `:app` layers

### UI
Jetpack Compose, one Activity, no fragment back stack. `MainScreen` owns the
`NavHost`. State is exposed from ViewModels as `StateFlow`.

One caveat worth knowing: screens reached through `composable { }` that take
`caseViewModel: CaseViewModel = hiltViewModel()` get a **`NavBackStackEntry`-scoped
instance**, not the Activity-scoped one `MainScreen` threads into its other
screens. Fields mirrored from a `@Singleton` repository resync and hide this;
plain per-instance `StateFlow`s such as `cleanupSuggestions` do not. Pass the
ViewModel explicitly.

### Domain
Business logic sits in service classes (`OcrProcessingService`, `ScriptRunner`,
`CleanupService`, `PackagingService`) and, increasingly, in `:shared`.

### Data
`StorageService` is the interface; `LocalFileStorageService` is the binding.
Repositories sit above it. Cloud providers implement `CloudStorageProvider`.

## Storage

**Local-first.** All user data lives in a single `lexorcist_data.xlsx` in the
app's storage directory, written with Apache POI. `LocalFileStorageService` is
the only thing that touches it.

Three properties that were not true before and are now:

- **Serialised.** Reads and writes hold a `Mutex`. `VideoProcessingWorker`
  writes evidence from a WorkManager job while the user edits in the
  foreground; without the lock those read-modify-write cycles interleaved and
  silently dropped one side's changes.
- **Atomic.** A save writes to a sibling `.tmp`, fsyncs, promotes the current
  database to `.bak`, then renames into place. Nothing ever truncates the live
  database.
- **Non-destructive on corruption.** An unreadable database is restored from
  `.bak` where possible, and otherwise moved aside as
  `lexorcist_data.xlsx.corrupt`. It is never deleted.

The honest assessment of this substrate is in `docs/performance.md`: the
spreadsheet was chosen for portability, but portability is a property of an
*export format*, not of a storage engine, and using it as the engine costs
transactions, indexes, per-row writes and schema migration. An embedded
database with an `.xlsx` export would serve the same goal better.

### Cloud

Google Drive is used as **whole-file backup storage** for the workbook and case
folders, via `SyncManager`. It is not a row-level database.

Note that a large part of `GoogleApiService` implements an abandoned
"Sheets as a row-addressed database" design — the case registry, per-case
evidence sheets, and row-level add/update/delete. Its only entry point is
`SpreadsheetImportService`, which has no `@Inject` constructor, no Hilt
provider and no caller, so none of it runs. Do not reason about the app's
behaviour from that code.

Sync is whole-file last-writer-wins with no merge. Two devices editing
different cases offline will lose one side's work on the second sync. This is a
known limitation, not a design intent.

## Key technologies

| Concern | Choice |
| --- | --- |
| Language / toolchain | Kotlin 2.3.21 on JDK 21 |
| Multiplatform | Kotlin Multiplatform, Compose Multiplatform 1.11.1 |
| Android build | AGP 9.3.1, Gradle 9.7.1 |
| DI (`:app` only) | Hilt |
| Async | Coroutines and Flow |
| Local database | Apache POI `.xlsx` |
| Preferences | DataStore; Tink / EncryptedSharedPreferences for credentials |
| OCR | ML Kit on-device text recognition |
| Speech to text | Vosk on-device |
| Embeddings | MediaPipe `TextEmbedder` |
| Scripting | Mozilla Rhino |
| Documents | iText |

Kotlin is held at 2.3.21 rather than 2.4.10 because KSP has no 2.4.x release and
Hilt's annotation processor requires KSP. It is the only dependency deliberately
behind latest stable, and a dependency forces it.

## Security

- **Script sandbox.** `ScriptRunner` installs a `ClassShutter` that blocks all
  Java class access. It does **not** block network egress: the sandbox
  deliberately exposes `lex.ai.generate` (a cloud Gemini call) and
  `lex.google.runAppsScript` (an Apps Script Execution API call made with the
  user's own OAuth credential). A script obtained from the shared "Extras"
  spreadsheet runs with those capabilities. Treat the class-access block as one
  control, not as isolation.
- **Path sanitisation.** `sanitizeSafePathSegment` restricts path segments to
  alphanumerics, dashes and underscores.
- **Formula injection.** `SpreadsheetUtils.sanitizeForSpreadsheet` prefixes any
  value starting with `=`, `+`, `-` or `@` with an apostrophe. Covered by
  `FormulaInjectionTest`.
- **Credentials at rest.** OAuth tokens go through `TinkSecureStorage`
  (AES-256-GCM, Android Keystore).
- **Evidence at rest is not encrypted**, and `android:allowBackup="true"` with
  backup rules that include all app files means evidence and the case database
  are included in Android Auto Backup. Both are open items.

## Data flow

1. The user captures or imports evidence.
2. A ViewModel delegates to a service (`OcrProcessingService`,
   `VoskTranscriptionService`, `VideoProcessingService`).
3. The service extracts text, hashes the original, and resolves a document
   date. **If no date can be established it records
   `OcrProcessingService.DATE_NOT_ESTABLISHED` (`0L`)** — never the current
   time. Anything rendering a chronology must exclude or label those items.
4. Active scripts run over the extracted text and apply tags.
5. `LocalFileStorageService` persists the row and copies the original into the
   case's raw folder.
6. Repositories emit through `Flow`; ViewModels map to `StateFlow`; Compose
   recomposes.

## Directory layout

```
shared/src/commonMain/kotlin/com/hereliesaz/lexorcist/
  core/          LexResult, LexError
  domain/model/  Evidence, Case, Exhibit, Allegation, Script, templates
  domain/parse/  DateExtractor
  ui/theme/      Compose Multiplatform theme
  ui/timeline/   Multiplatform timeline

app/src/main/java/com/hereliesaz/lexorcist/
  di/            Hilt modules
  ui/            Composable screens and components
  viewmodel/     ViewModels
  data/          Repositories, storage, cloud providers
  service/       OCR, transcription, scripting, Google APIs
  utils/         Helpers
```
