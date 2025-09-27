# How to Configure Google API for Gmail Import

To enable the "Import from Gmail" feature in this application, you must configure a project in the Google API Console, enable the Gmail API, and provide the necessary credentials to the app.

Follow these steps carefully:

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

Before you can create credentials, you need to configure the OAuth consent screen, which is what users will see when they grant your app permission to access their data.

1.  **Go to the OAuth Consent Screen Page**:
    *   In the navigation menu, go to **"APIs & Services" > "OAuth consent screen"**.
2.  **Choose User Type**:
    *   Select **"External"** and click **"Create"**. (If you have a Google Workspace account, you can choose "Internal," but "External" is the most common choice).
3.  **Fill in App Information**:
    *   **App name**: Enter the name of your application (e.g., "The Lexorcist").
    *   **User support email**: Select your email address.
    *   **Developer contact information**: Enter your email address.
    *   Click **"Save and Continue"**.
4.  **Add Scopes**:
    *   Click **"Add or Remove Scopes"**.
    *   In the filter, search for the following scopes and check the box for each one:
        *   `.../auth/drive` (for Google Drive integration)
        *   `.../auth/spreadsheets` (for Google Sheets integration)
        *   `.../auth/gmail.readonly` (for reading emails)
        *   `.../auth/userinfo.email`
        *   `.../auth/userinfo.profile`
    *   Click **"Update"**.
    *   Click **"Save and Continue"**.
5.  **Add Test Users**:
    *   For now, you must add the email addresses of the users who will be testing the app. Click **"Add Users"** and enter the Google account email address(es) you will be using to sign in.
    *   Click **"Save and Continue"**.
    *   Review the summary and click **"Back to Dashboard"**.

### 4. Create Android Credentials

1.  **Go to the Credentials Page**:
    *   In the navigation menu, go to **"APIs & Services" > "Credentials"**.
2.  **Create Credentials**:
    *   Click **"+ Create Credentials"** and select **"OAuth client ID"**.
3.  **Configure the OAuth Client ID**:
    *   **Application type**: Select **"Android"**.
    *   **Package name**: Enter the package name of this application. You can find this in the `build.gradle.kts` file (look for `applicationId`). It should be `com.hereliesaz.lexorcist`.
    *   **SHA-1 certificate fingerprint**: This is crucial for securing your app. You need to get the SHA-1 fingerprint of your signing certificate.
        *   You can get this by running the following command in the project's root directory:
            ```bash
            ./gradlew signingReport
            ```
        *   Look for the "SHA1" value under the "debug" configuration and copy it.
    *   Click **"Create"**.

### 5. Update the Application

1.  **Update `default_web_client_id`**:
    *   After creating the Android credentials, you should also have a **Web client ID** in your credentials list (it's often created automatically, or you may need to create a "Web application" type credential).
    *   Copy the **Client ID** from the **Web client** (NOT the Android one).
    *   Open `app/src/main/res/values/strings.xml` and update the `default_web_client_id` string with this value.
2.  **You're Done!** The application should now be configured to use the Gmail API.

**IMPORTANT**: You must complete all of these steps for the email import functionality to work.