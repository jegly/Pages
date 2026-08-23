plugins {
    alias(libs.plugins.pages.android.library)
    alias(libs.plugins.pages.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.pages.android.room)
}

android {
    namespace = "com.jegly.pages.core.data"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.model)


    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.testing)
}
