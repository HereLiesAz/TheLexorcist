# Cloud Provider Setup — Dropbox & OneDrive (Azure)

> **Status:** Dropbox is implemented and enabled (PKCE auth + refresh tokens, credential
> encrypted with Tink). Its app key is already wired in (`f0uqbas5hc8zihy`). OneDrive is
> **parked for now** — the steps below remain for when it's revived. End-to-end verification of
> Dropbox still requires a real device + Dropbox account; `TinkSecureStorage` is covered by an
> instrumented test (Android Keystore), and `DropboxAuthManager` by JVM unit tests.


These are the **external, one-time registration steps** needed before Dropbox and OneDrive
sync will work. The app code uses placeholders today; this guide tells you exactly what to
create and where each value goes.

Package name (used in both): **`com.hereliesaz.lexorcist`**

---

## Part 1 — Generate your app signing-key SHA-1 / signature hash (needed by both)

Both Dropbox (optional) and Azure (required) tie the OAuth redirect to your signing key, so
a stolen client id/app key can't be used by another app.

**Debug key** (for development builds):
```bash
keytool -exportcert -alias androiddebugkey \
  -keystore ~/.android/debug.keystore -storepass android -keypass android \
  | openssl sha1 -binary | openssl base64
```

**Release key** (for the Play build — required for production):
```bash
keytool -exportcert -alias <YOUR_KEY_ALIAS> -keystore <YOUR_RELEASE_KEYSTORE> \
  | openssl sha1 -binary | openssl base64
```
(The release alias is `key0` per `app/build.gradle.kts`; supply your real keystore + passwords.)

Save both outputs — the Base64 string is your **signature hash**. You must register **both**
the debug and release hashes (Azure lets you add multiple; add the release one before shipping).

---

## Part 2 — Dropbox

### 2.1 Create the app
1. Go to <https://www.dropbox.com/developers/apps> → **Create app**.
2. **Choose an API**: *Scoped access*.
3. **Type of access**: *App folder* (recommended — the app only sees its own folder, which is
   what the sync code expects). *Full Dropbox* also works if you want broader access.
4. Name the app (must be globally unique, e.g. `the-lexorcist-<you>`), then **Create app**.

### 2.2 Permissions (scopes)
On the app's **Permissions** tab, enable and **Submit**:
- `account_info.read`
- `files.metadata.read`
- `files.content.read`
- `files.content.write`

### 2.3 Settings
On the **Settings** tab:
- Copy the **App key** (this is the only secret the mobile app needs — PKCE means **no app
  secret** is embedded in the app).
- Confirm **Access token expiration** is *Short-lived* (the default). Short-lived + PKCE is what
  issues the **refresh token** the app stores; long-lived tokens are deprecated.

### 2.4 Where the values go (in this repo)
- `app/src/main/res/values/strings.xml` → replace the placeholder:
  ```xml
  <string name="dropbox_app_key">YOUR_DROPBOX_APP_KEY</string>
  ```
- `app/src/main/AndroidManifest.xml` → the Dropbox `AuthActivity` needs the redirect scheme
  `db-<APP_KEY>` so the browser returns to the app. Update the existing entry to:
  ```xml
  <activity
      android:name="com.dropbox.core.android.AuthActivity"
      android:exported="true"
      android:launchMode="singleTask"
      tools:replace="android:exported">
      <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.BROWSABLE" />
          <category android:name="android.intent.category.DEFAULT" />
          <!-- Replace YOUR_DROPBOX_APP_KEY (keep the db- prefix). -->
          <data android:scheme="db-YOUR_DROPBOX_APP_KEY" />
      </intent-filter>
  </activity>
  ```

> Note: the Dropbox console does **not** need a redirect URI for the Android SDK PKCE flow —
> the `db-<APP_KEY>` custom scheme above is the redirect.

---

## Part 3 — OneDrive (Microsoft Entra / Azure AD)

### 3.1 Register the application
1. Go to <https://entra.microsoft.com> → **App registrations** → **New registration**
   (or Azure Portal → *Microsoft Entra ID* → *App registrations*).
2. **Name**: e.g. `The Lexorcist`.
3. **Supported account types**: *Accounts in any organizational directory and personal Microsoft
   accounts* (this matches the app's `AzureADandPersonalMicrosoftAccount` config and lets personal
   OneDrive accounts sign in).
4. Skip the Redirect URI for now → **Register**.
5. Copy the **Application (client) ID** — you'll paste it into `msal_config.json`.

### 3.2 Add the Android platform (this generates your redirect URI)
1. In the app → **Authentication** → **Add a platform** → **Android**.
2. **Package name**: `com.hereliesaz.lexorcist`.
3. **Signature hash**: paste the Base64 hash from Part 1 (add the debug one now; add the release
   one before shipping).
4. Azure now shows the exact **MSAL redirect URI** and a config snippet — **copy them verbatim**;
   they have the right URL-encoding (format:
   `msauth://com.hereliesaz.lexorcist/<url-encoded-signature-hash>`).

### 3.3 API permissions
**API permissions** → **Add a permission** → **Microsoft Graph** → **Delegated permissions**:
- `User.Read`
- `Files.ReadWrite`  *(use `Files.ReadWrite.All` only if you need shared/all files)*
- `offline_access`  *(required for refresh tokens / silent token renewal)*

Click **Grant admin consent** if you're testing against an org tenant (personal accounts consent
at sign-in).

### 3.4 Where the values go (in this repo)
- `app/src/main/res/raw/msal_config.json`:
  ```jsonc
  {
    "client_id": "<APPLICATION_CLIENT_ID>",
    "authorization_user_agent": "DEFAULT",
    "redirect_uri": "<PASTE THE REDIRECT URI AZURE GENERATED>",
    "account_mode": "SINGLE",
    "broker_redirect_uri_registered": false,   // false = no Authenticator/Company Portal broker
    "authorities": [
      { "type": "AAD", "audience": { "type": "AzureADandPersonalMicrosoftAccount", "tenant_id": "common" } }
    ]
  }
  ```
- `app/src/main/AndroidManifest.xml` → set the MSAL `BrowserTabActivity` data to match the
  signature hash (replace `/YOUR_BASE64_SIGNATURE_HASH`):
  ```xml
  <data
      android:host="com.hereliesaz.lexorcist"
      android:path="/<YOUR_BASE64_SIGNATURE_HASH>"
      android:scheme="msauth" />
  ```
  If you copied an MSAL config snippet from Azure, use the exact `android:path` it shows.

> Tip: the Azure portal's Android quickstart generates both the `redirect_uri` for the JSON and
> the `<activity>` manifest block. Copying those directly avoids URL-encoding mistakes.

---

## Part 4 — Verify

1. Build a debug APK and sign in to each provider from Settings (once the
   `showExperimentalCloudProviders` flag is enabled and the providers are implemented).
2. Dropbox: after the browser hands back, `MainActivity.onResume()` captures the credential.
3. OneDrive: MSAL completes via the `BrowserTabActivity` redirect.
4. Before Play release: register the **release** signature hash in Azure (and Dropbox if you add a
   release-specific redirect), and confirm sign-in works on a release-signed build.

## Security reminders
- The Dropbox **App key** and Azure **client id** are public client identifiers (safe to embed),
  but the **signature-hash binding** is what prevents impersonation — keep your keystores private.
- Do **not** commit a real `google-services.json` (see `scripts/scrub-leaked-key-history.sh`).
- Tokens are stored encrypted (Tink) once the hardening work lands; never log them.
