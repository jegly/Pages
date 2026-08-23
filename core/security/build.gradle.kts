plugins {
    alias(libs.plugins.pages.android.library)
    alias(libs.plugins.pages.hilt)
}

android {
    namespace = "com.jegly.pages.core.security"
}

dependencies {
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
}
