plugins {
    alias(libs.plugins.pages.android.library)
    alias(libs.plugins.pages.hilt)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-proguard-rules.pro")
    }
    namespace = "com.jegly.pages.core.datastore"
}

dependencies {
    api(projects.core.model)

    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
