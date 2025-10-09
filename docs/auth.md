# Authentication Setup

This document provides instructions for configuring authentication services to enable email import features in The Lexorcist. Follow the guide for the service you wish to integrate.

---

## Google API for Gmail Import

To enable the "Import from Gmail" feature, you must configure a project in the Google API Console, enable the Gmail API, and provide the necessary credentials to the app.

### 1. Set Up Your Google Cloud Project

1.  **Go to the Google API Console**: Open your web browser and navigate to the [Google API Console](https://console.developers.google.com/).
2.  **Create a New Project**:
    *   Click the project dropdown in the top-left corner and select **"New Project"**.
    *   Give your project a descriptive name (e.g., "Lexorcist Gmail Integration") and click **"Create"**.

### 2. Enable the Gmail API

1.  **Go to the API Library**:
    *   Make sure your new project is selected in the project dropdown.
    *   In the navigation menu on the left, go to **"APIs & Services" > "Library"**.
2.  **Find and Enable the Gmail API**:
    *   In the search bar, type "Gmail API" and press Enter.
    *   Click on the **"Gmail API"** result.
    *   Click the **"Enable"** button.

### 3. Configure the OAuth Consent Screen

1.  **Go to the OAuth Consent Screen Page**:
    *   In the navigation menu, go to **"APIs & Services" > "OAuth consent screen"**.
2.  **Choose User Type**:
    *   Select **"External"** and click **"Create"**.
3.  **Fill in App Information**:
    *   **App name**: Enter "The Lexorcist".
    *   **User support email**: Select your email address.
    *   **Developer contact information**: Enter your email address.
    *   Click **"Save and Continue"**.
4.  **Add Scopes**:
    *   Click **"Add or Remove Scopes"**.
    *   Add the following scopes:
        *   `.../auth/drive`
        *   `.../auth/spreadsheets`
        *   `.../auth/gmail.readonly`
        *   `.../auth/userinfo.email`
        *   `.../auth/userinfo.profile`
    *   Click **"Update"**, then **"Save and Continue"**.
5.  **Add Test Users**:
    *   Add the email addresses of your test users.
    *   Click **"Save and Continue"**.

### 4. Create Android Credentials

1.  **Go to the Credentials Page**:
    *   Navigate to **"APIs & Services" > "Credentials"**.
2.  **Create Credentials**:
    *   Click **"+ Create Credentials"** and select **"OAuth client ID"**.
3.  **Configure the OAuth Client ID**:
    *   **Application type**: Select **"Android"**.
    *   **Package name**: `com.hereliesaz.lexorcist`.
    *   **SHA-1 certificate fingerprint**: Run `./gradlew signingReport` in the project root and copy the "SHA1" value from the "debug" configuration.
    *   Click **"Create"**.

### 5. Update the Application

1.  Copy the **Client ID** from the **Web client** credential (not the Android one).
2.  Open `app/src/main/res/values/strings.xml` and update the `default_web_client_id` string with this value.

---

## Azure AD for Outlook/Microsoft Graph API Import

To enable the "Import from Outlook" feature, you must register an application in the Azure portal.

### 1. Register a New Application in Azure AD

1.  **Go to the Azure Portal**: Navigate to the [Azure portal](https://portal.azure.com/).
2.  **Navigate to App Registrations**: Search for and select "App registrations".
3.  **Create a New Registration**:
    *   Click **"+ New registration"**.
    *   **Name**: "The Lexorcist Android App".
    *   **Supported account types**: Select **"Accounts in any organizational directory...and personal Microsoft accounts"**.
    *   **Redirect URI**:
        *   Click **"+ Add a platform"** -> **"Android"**.
        *   **Package name**: `com.hereliesaz.lexorcist`.
        *   **Signature hash**: Generate this by converting your SHA-1 fingerprint (from the Google setup) to Base64. Use an online tool or OpenSSL: `echo YOUR_SHA1_HERE | xxd -r -p | openssl base64`.
        *   Click **"Configure"**.

### 2. Add API Permissions

1.  **Go to API Permissions** in your app registration.
2.  **Add a Permission**:
    *   Click **"+ Add a permission"** -> **"Microsoft Graph"**.
    *   Select **"Delegated permissions"**.
    *   Search for and add the **`Mail.Read`** permission.

### 3. Create MSAL Configuration File

1.  In `app/src/main/res/raw`, create a file named `auth_config_single_account.json`.
2.  Go to the **"Overview"** page of your app registration in Azure and copy the **"Application (client) ID"**.
3.  Populate the JSON file with the following content, replacing `YOUR_CLIENT_ID` and `YOUR_BASE64_SIGNATURE_HASH`:

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