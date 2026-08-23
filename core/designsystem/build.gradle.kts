plugins {
    alias(libs.plugins.pages.android.library)
    alias(libs.plugins.pages.android.library.compose)
}

android {
    namespace = "com.jegly.pages.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.material3.adaptive)
    api(libs.androidx.compose.material3.adaptive.navigationsuite)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui.util)
}
