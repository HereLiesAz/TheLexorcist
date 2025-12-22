## 2025-07-30 - [EncryptedSharedPreferences Implementation]
**Vulnerability:** Sensitive auth tokens (OneDrive, Dropbox) and user emails were stored in plaintext in standard SharedPreferences, accessible via root or backup extraction.
**Learning:** Replacing the SharedPreferences implementation with `EncryptedSharedPreferences` via DI is a seamless way to secure existing data access points without refactoring consuming classes. However, switching to encrypted storage changes the file format, necessitating a new filename to avoid crash loops on startup, which results in data loss (clearing preferences) for existing users.
**Prevention:** Always use `EncryptedSharedPreferences` for storing sensitive data from the start of the project to avoid painful migrations later.
