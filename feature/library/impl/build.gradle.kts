plugins {
    alias(libs.plugins.pages.android.feature.impl)
    alias(libs.plugins.pages.android.library.compose)
}

android {
    namespace = "com.jegly.pages.feature.library.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.security)
    implementation(projects.feature.library.api)
    implementation(projects.feature.reader.api)
    implementation(libs.androidx.biometric)

    testImplementation(projects.core.testing)

    androidTestImplementation(libs.bundles.androidx.compose.ui.test)
    androidTestImplementation(projects.core.testing)
}
