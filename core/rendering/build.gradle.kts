plugins {
    alias(libs.plugins.pages.android.library)
}

android {
    namespace = "com.jegly.pages.core.rendering"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
