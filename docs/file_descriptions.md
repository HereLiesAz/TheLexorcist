# File Descriptions

This file provides a brief but thorough description of what each non-ignored file in the project is supposed to do.

## Root Directory

This directory contains the main project files, including configuration, documentation, and the main application module.

*   `CONTRIBUTING.md`: Provides guidelines for contributing to the project, including code style, pull request process, and other conventions.
*   `PRIVACY_POLICY.md`: The privacy policy for the application, which outlines how user data is collected, used, and protected.
*   `PRIVACY_POLICY_ES.md`: The privacy policy in Spanish.
*   `README.md`: The main README file for the project, containing an overview, setup instructions, and other important information. This is the first file a new developer should read.
*   `SCRIPT_EXAMPLES.md`: Contains examples of scripts that can be used with the application's scripting functionality.
*   `TERMS_OF_SERVICE.md`: The terms of service for the application, which define the rules and regulations for using the app.
*   `TERMS_OF_SERVICE_ES.md`: The terms of service in Spanish.
*   `TODO.md`: A file that tracks the project's roadmap and tasks, providing a high-level overview of the project's future direction.
*   `app/`: The main application module, containing all the source code, resources, and assets for the Android app.
*   `assets/`: Contains image assets for the project, such as logos and icons that are used in the web interface.
*   `build.gradle.kts`: The main build script for the project, which configures the build process for all modules.
*   `docs/`: Contains documentation for the project, including this file, as well as architectural diagrams and other important information.
*   `gradle/`: Contains the Gradle wrapper files, which allow the project to be built without installing Gradle locally.
*   `gradle.properties`: Configuration file for Gradle, which can be used to set project-wide properties.
*   `gradlew`: The Gradle wrapper script for Unix-based systems.
*   `gradlew.bat`: The Gradle wrapper script for Windows.
*   `index.html`: The main HTML file for the web interface, which serves as the entry point for the web app.
*   `ms-identity-android-kotlin-master/`: A submodule or directory related to Microsoft identity integration, which is used for authentication.
*   `ms-identity-android-kotlin.zip`: A zip file related to Microsoft identity integration.
*   `settings.gradle.kts`: The settings script for Gradle, which defines the project structure and includes all the modules.
*   `web/`: Contains web-related files, such as CSS and JavaScript, for the web interface.
*   `whisper/`: A directory related to the Whisper speech recognition library, which is used for audio transcription.

## `app/` Directory

This directory contains the main application module, which is the heart of the Android app.

*   `build.gradle.kts`: The build script for the `app` module, which contains dependencies for libraries like AndroidX, Hilt, and Retrofit, as well as build configurations for different build types and product flavors.
*   `google-services.template.json`: A template for the `google-services.json` file, which is required for Google services integration, such as Firebase and Google Sign-In.
*   `lint-baseline.xml`: A baseline file for Android Lint, which tracks the current set of lint warnings and allows the build to pass even if new warnings are introduced.
*   `src/`: The source code directory for the `app` module, which is organized into different source sets for main, test, and androidTest.

## `app/src/` Directory

This directory contains the source code for the application, organized into three main source sets.

*   `androidTest/`: Contains Android instrumentation tests, which run on an Android device or emulator and are used to test the UI and other components that require an Android context.
*   `main/`: Contains the main source code for the application, including the manifest, resources, and Java/Kotlin code.
*   `test/`: Contains unit tests, which run on the JVM and are used to test the business logic of the application.

## `app/src/androidTest/` Directory

This directory contains the instrumentation tests for the application.

*   `java/`: The root directory for the Java/Kotlin source code of the instrumentation tests.

## `app/src/androidTest/java/com/hereliesaz/lexorcist/` Directory

This directory contains the instrumentation tests for the application.

*   `EvidenceProcessingTest.kt`: Tests for evidence processing, which ensure that the app can correctly process different types of evidence.
*   `ExampleInstrumentedTest.kt`: An example instrumentation test, which can be used as a template for new tests.
*   `HiltTestRunner.kt`: A custom test runner for Hilt, which is used to enable dependency injection in tests.
*   `di/`: A directory for dependency injection related to tests, which contains modules that provide fake or mock dependencies for testing.
*   `service/`: A directory for service-related tests, which test the application's services, such as the `TranscriptionService` and `OcrProcessingService`.
*   `viewmodel/`: A directory for ViewModel-related tests, which test the application's ViewModels.

## `app/src/main/` Directory

This directory contains the main source code for the application.

*   `AndroidManifest.xml`: The manifest file for the application, which declares the application's components, permissions, and other essential information.
*   `assets/`: Contains asset files, such as CSV files, JSON files, and other data that is bundled with the app.
*   `ic_launcher-playstore.png`: The application icon for the Google Play Store.
*   `java/`: The root directory for the Java/Kotlin source code of the application.
*   `res/`: Contains resource files, such as layouts, drawables, and strings, which are used to build the application's UI.

## `app/src/main/assets/` Directory

This directory contains asset files that are bundled with the app.

*   `allegations.csv`: A CSV file containing a list of allegations, which is used to populate the allegations screen.
*   `allegations_catalog.json`: A JSON file containing a catalog of allegations, which provides additional information about each allegation.
*   `civil_weights.json`: A JSON file containing statistical weights for civil cases, which are used in the app's legal analysis features.
*   `criminal_weights.json`: A JSON file containing statistical weights for criminal cases, which are used in the app's legal analysis features.
*   `default_scripts.csv`: A CSV file containing default scripts, which can be used to automate tasks in the app.
*   `default_templates.csv`: A CSV file containing default templates, which can be used to generate documents.
*   `evidence_catalog.json`: A JSON file containing a catalog of evidence types, which is used to classify evidence.
*   `exhibits.csv`: A CSV file containing a list of exhibits, which is used to populate the exhibits screen.
*   `exhibits_catalog.json`: A JSON file containing a catalog of exhibit types, which provides additional information about each exhibit.
*   `jurisdictions.csv`: A CSV file containing a list of jurisdictions, which is used to filter legal information by jurisdiction.
*   `jurisdictions.json`: A JSON file containing a list of jurisdictions.
*   `legal_data_map.json`: A JSON file containing a map of legal data, which is used to link different legal concepts together.
*   `statistical_weights.json`: A JSON file containing statistical weights, which are used in the app's legal analysis features.
*   `vocab.txt`: A text file containing a vocabulary list, which is used for natural language processing.

## `app/src/main/java/com/hereliesaz/lexorcist/` Directory

This directory contains the main Java/Kotlin source code for the application.

*   `GetContentWithMultiFilter.kt`: A helper class for getting content with multiple filters, which is used to filter data from various sources.
*   `Lexorcist.kt`: A core class for the Lexorcist application, which contains the main application logic.
*   `LexorcistApplication.kt`: The main application class, which is responsible for initializing the application and setting up dependency injection.
*   `MainActivity.kt`: The main activity of the application, which serves as the entry point for the UI.
*   `MainScreen.kt`: The main screen of the application, which is a composable function that defines the layout of the main screen.
*   `SpreadsheetImportService.kt`: A service for importing data from spreadsheets, which is used to import case data from external sources.
*   `auth/`: A directory for authentication-related classes, which handle user authentication with services like Google and Microsoft.
*   `common/`: A directory for common classes and utilities that are used throughout the application.
*   `data/`: A directory for data-related classes, such as repositories and data models, which are responsible for managing the application's data.
*   `di/`: A directory for dependency injection modules, which are used by Hilt to provide dependencies to different parts of the application.
*   `fragments/`: A directory for fragments, which are used to build the UI of the application.
*   `model/`: A directory for data models, which define the structure of the data used in the application.
*   `service/`: A directory for services, which perform long-running operations in the background, such as processing evidence and syncing data.
*   `ui/`: A directory for UI-related classes, such as activities, composables, and themes, which are responsible for the application's look and feel.
*   `utils/`: A directory for utility classes, which provide helper functions for common tasks.
*   `viewmodel/`: A directory for ViewModels, which are responsible for preparing and managing the data for the UI.
