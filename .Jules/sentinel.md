## 2024-05-23 - Formula Injection in Spreadsheets
**Vulnerability:** User-controlled input (Case names, Evidence content, etc.) was written directly to XLSX cells without sanitization. If a user entered a string starting with `=`, `+`, `-`, or `@`, it could be interpreted as a malicious formula by Excel (CSV Injection / Formula Injection).
**Learning:** Even "internal" files like local spreadsheets are attack vectors if they process untrusted input and are opened by rich clients like Excel.
**Prevention:** Implemented a `sanitizeForSpreadsheet` helper in `LocalFileStorageService.kt` that prepends a single quote `'` to any string starting with hazardous characters, forcing Excel to treat it as text. Applied this to all relevant write operations.
