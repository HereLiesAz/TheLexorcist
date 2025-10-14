# File Descriptions

This file provides a brief but thorough description of what each non-ignored file in the project is supposed to do.

## Root Directory

*   `CONTRIBUTING.md`: Provides guidelines for contributing to the project.
*   `PRIVACY_POLICY.md`: The privacy policy for the application.
*   `PRIVACY_POLICY_ES.md`: The privacy policy in Spanish.
*   `README.md`: The main README file for the project, containing an overview, setup instructions, and other important information.
*   `SCRIPT_EXAMPLES.md`: Contains examples of scripts that can be used with the application.
*   `TERMS_OF_SERVICE.md`: The terms of service for the application.
*   `TERMS_OF_SERVICE_ES.md`: The terms of service in Spanish.
*   `TODO.md`: A file that tracks the project's roadmap and tasks.
*   `app/`: The main application module.
*   `assets/`: Contains image assets for the project.
*   `build.gradle.kts`: The main build script for the project.
*   `docs/`: Contains documentation for the project.
*   `gradle/`: Contains the Gradle wrapper files.
*   `gradle.properties`: Configuration file for Gradle.
*   `gradlew`: The Gradle wrapper script for Unix-based systems.
*   `gradlew.bat`: The Gradle wrapper script for Windows.
*   `index.html`: The main HTML file for the web interface.
*   `ms-identity-android-kotlin-master/`: A submodule or directory related to Microsoft identity integration.
*   `ms-identity-android-kotlin.zip`: A zip file related to Microsoft identity integration.
*   `settings.gradle.kts`: The settings script for Gradle, which defines the project structure.
*   `web/`: Contains web-related files.
*   `whisper/`: A directory related to the Whisper speech recognition library.

## `app/` Directory

*   `build.gradle.kts`: The build script for the `app` module, containing dependencies and build configurations.
*   `google-services.template.json`: A template for the `google-services.json` file, which is required for Google services integration.
*   `lint-baseline.xml`: A baseline file for Android Lint, which tracks the current set of lint warnings.
*   `src/`: The source code directory for the `app` module.

## `app/src/` Directory

*   `androidTest/`: Contains Android instrumentation tests.
*   `main/`: Contains the main source code for the application.
*   `test/`: Contains unit tests.

## `app/src/androidTest/` Directory

*   `java/`: The root directory for the Java/Kotlin source code of the instrumentation tests.

## `app/src/androidTest/java/com/hereliesaz/lexorcist/` Directory

*   `EvidenceProcessingTest.kt`: Tests for evidence processing.
*   `ExampleInstrumentedTest.kt`: An example instrumentation test.
*   `HiltTestRunner.kt`: A custom test runner for Hilt.
*   `di/`: A directory for dependency injection related to tests.
*   `service/`: A directory for service-related tests.
*   `viewmodel/`: A directory for ViewModel-related tests.

## `app/src/main/` Directory

*   `AndroidManifest.xml`: The manifest file for the application.
*   `assets/`: Contains asset files, such as CSV files, JSON files, and other data.
*   `ic_launcher-playstore.png`: The application icon for the Google Play Store.
*   `java/`: The root directory for the Java/Kotlin source code.
*   `res/`: Contains resource files, such as layouts, drawables, and strings.

## `app/src/main/assets/` Directory

*   `allegations.csv`: A CSV file containing a list of allegations.
*   `allegations_catalog.json`: A JSON file containing a catalog of allegations.
*   `civil_weights.json`: A JSON file containing statistical weights for civil cases.
*   `criminal_weights.json`: A JSON file containing statistical weights for criminal cases.
*   `default_scripts.csv`: A CSV file containing default scripts.
*   `default_templates.csv`: A CSV file containing default templates.
*   `evidence_catalog.json`: A JSON file containing a catalog of evidence types.
*   `exhibits.csv`: A CSV file containing a list of exhibits.
*   `exhibits_catalog.json`: A JSON file containing a catalog of exhibit types.
*   `jurisdictions.csv`: A CSV file containing a list of jurisdictions.
*   `jurisdictions.json`: A JSON file containing a list of jurisdictions.
*   `legal_data_map.json`: A JSON file containing a map of legal data.
*   `statistical_weights.json`: A JSON file containing statistical weights.
*   `vocab.txt`: A text file containing a vocabulary list.

## `app/src/main/java/com/hereliesaz/lexorcist/` Directory

*   `GetContentWithMultiFilter.kt`: A helper class for getting content with multiple filters.
*   `Lexorcist.kt`: A core class for the Lexorcist application.
*   `LexorcistApplication.kt`: The main application class.
*   `MainActivity.kt`: The main activity of the application.
*   `MainScreen.kt`: The main screen of the application.
*   `SpreadsheetImportService.kt`: A service for importing data from spreadsheets.
*   `auth/`: A directory for authentication-related classes.
*   `common/`: A directory for common classes and utilities.
*   `data/`: A directory for data-related classes, such as repositories and data models.
*   `di/`: A directory for dependency injection modules.
*   `fragments/`: A directory for fragments.
*   `model/`: A directory for data models.
*   `service/`: A directory for services.
*   `ui/`: A directory for UI-related classes, such as activities, composables, and themes.
*   `utils/`: A directory for utility classes.
*   `viewmodel/`: A directory for ViewModels.
