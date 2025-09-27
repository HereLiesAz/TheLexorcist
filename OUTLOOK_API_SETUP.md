# How to Configure Azure AD for Outlook/Microsoft Graph API Import

To enable the "Import from Outlook" feature, you must register your application in the Azure portal. This will provide your app with the necessary credentials to authenticate with the Microsoft identity platform and access user data via the Microsoft Graph API.

Follow these steps carefully:

### 1. Register a New Application in Azure Active Directory

1.  **Go to the Azure Portal**: Open your web browser and navigate to the [Azure portal](https://portal.azure.com/).
2.  **Navigate to App Registrations**:
    *   In the search bar at the top, type "App registrations" and select it from the results.
3.  **Create a New Registration**:
    *   Click **"+ New registration"**.
4.  **Configure the Registration**:
    *   **Name**: Give your application a descriptive name (e.g., "The Lexorcist Android App").
    *   **Supported account types**: Select **"Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)"**. This is crucial to allow personal Outlook.com accounts to sign in.
    *   **Redirect URI**: This is a key step for the Android app.
        *   Click **"+ Add a platform"** and select **"Android"**.
        *   **Package name**: Enter the package name of this application: `com.hereliesaz.lexorcist`.
        *   **Signature hash**: You need to generate a signature hash for your app's signing certificate.
            *   **Get the SHA-1 Fingerprint**: Follow the instructions in `GMAIL_API_SETUP.md` to get your SHA-1 fingerprint using the `./gradlew signingReport` command.
            *   **Convert SHA-1 to Base64**: Use an online converter or the following command (requires OpenSSL) to convert your hex SHA-1 to Base64. Replace `YOUR_SHA1_HERE` with your actual SHA-1 key (without the "SHA1:" prefix and with no colons):
                ```bash
                echo YOUR_SHA1_HERE | xxd -r -p | openssl base64
                ```
            *   Enter the resulting Base64-encoded signature hash into the field.
        *   Click **"Configure"**.

### 2. Add API Permissions

You need to grant your app permission to read user's mail.

1.  **Go to API Permissions**:
    *   In your app registration's navigation menu, click on **"API permissions"**.
2.  **Add a Permission**:
    *   Click **"+ Add a permission"**.
    *   Select **"Microsoft Graph"**.
3.  **Select Delegated Permissions**:
    *   Choose **"Delegated permissions"**. Your app will be accessing the API as the signed-in user.
4.  **Add Mail.Read Permission**:
    *   In the "Select permissions" search box, type `Mail.Read`.
    *   Check the box next to **`Mail.Read`**.
    *   Click **"Add permissions"**. The `User.Read` permission is usually added by default, which is also required.

### 3. Create a Configuration File for MSAL

MSAL for Android uses a JSON configuration file to know which app registration to use.

1.  **Create the JSON file**:
    *   In the `app/src/main/res/raw` directory of this project, create a new file named `auth_config_single_account.json`.
2.  **Get Your Application (client) ID**:
    *   Go back to the **"Overview"** page of your app registration in the Azure portal.
    *   Copy the **"Application (client) ID"**.
3.  **Populate the JSON File**:
    *   Paste the following content into `auth_config_single_account.json`, replacing `"YOUR_CLIENT_ID"` with the ID you just copied.

    ```json
    {
      "client_id": "YOUR_CLIENT_ID",
      "authorization_user_agent": "DEFAULT",
      "redirect_uri": "msauth://com.hereliesaz.lexorcist/YOUR_BASE64_SIGNATURE_HASH",
      "account_mode": "SINGLE",
      "authorities": [
        {
          "type": "AAD",
          "audience": {
            "type": "AzureADandPersonalMicrosoftAccount",
            "tenant_id": "common"
          }
        }
      ]
    }
    ```
    *   **IMPORTANT**: In the `redirect_uri` field, you must replace `YOUR_BASE64_SIGNATURE_HASH` with the same Base64-encoded signature hash you generated and used in the Azure portal.

**You're Done!** The application is now configured to authenticate with Microsoft and use the Graph API to import emails from Outlook.