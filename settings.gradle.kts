pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io"); content { includeGroupByRegex("com\\.github.*") } }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io"); content { includeGroupByRegex("com\\.github.*") } }
    }
}
rootProject.name = "pages"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":core:datastore")
include(":core:model")
include(":core:navigation")
include(":core:rendering")
include(":core:security")
include(":core:testing")

include(":feature:library:api")
include(":feature:library:impl")
include(":feature:reader:api")
include(":feature:reader:impl")
include(":feature:bookmarks:api")
include(":feature:bookmarks:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:settings:impl")
include(":core:tts")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    Pages requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}
