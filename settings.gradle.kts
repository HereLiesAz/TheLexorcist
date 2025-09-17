pluginManagement {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id == "io.objectbox") {
                    useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
                }
            }
        }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
        }
    }
}
rootProject.name = "The Lexorcist"
include(":app")
