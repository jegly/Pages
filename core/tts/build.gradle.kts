plugins {
    alias(libs.plugins.pages.android.library)
    alias(libs.plugins.pages.hilt)
}

android {
    namespace = "com.jegly.pages.core.tts"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.datastore)

    implementation(files("libs/sherpa-onnx-1.10.36.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.compiler)
}
