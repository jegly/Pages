plugins {
    alias(libs.plugins.pages.android.feature.impl)
    alias(libs.plugins.pages.android.library.compose)
}

android {
    namespace = "com.jegly.pages.feature.reader.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.security)
    implementation(projects.core.rendering)
    implementation(projects.core.tts)
    implementation(projects.feature.reader.api)
    implementation(projects.feature.bookmarks.api)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.compose.material3.adaptive.layout)

    testImplementation(projects.core.testing)
}
