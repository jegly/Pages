package com.jegly.pages.core.model

enum class AccentColor {
    Emerald, Amber, Sapphire, Ruby, Amethyst, Rose,
}

enum class ReadingTheme {
    System, Light, Dark, Sepia, Green, Night, Custom,
}

enum class PageTurnMode {
    Swipe, Scroll,
}

enum class SliderTimeout {
    Off,      // never shows
    Short,    // 2 s
    Medium,   // 3 s
    Long,     // 5 s
    AlwaysOn, // never hides
}

enum class PdfFillMode {
    FitPage,    // whole page visible, may have gaps top/bottom
    FillWidth,  // fills screen width, top-aligned, gap at bottom only
    FillScreen, // crops to fill entire screen, no gaps
}

enum class FontFamily {
    Default, Serif, Monospace, SansSerif,
}

data class UserPreferences(
    val accentColor: AccentColor = AccentColor.Sapphire,
    val fontScaleFactor: Float = 1.0f,
    val readingTheme: ReadingTheme = ReadingTheme.System,
    val useDynamicColor: Boolean = false,
    val lastOpenedBookUri: String? = null,
    // Reader display
    val lineSpacingMultiplier: Float = 1.4f,
    val horizontalMarginDp: Int = 16,
    val fontFamily: FontFamily = FontFamily.Default,
    val brightness: Float = -1f,       // -1 = system brightness
    val blueLightFilter: Boolean = false,
    val customBackgroundColor: Long? = null,
    // Layout
    val pageTurnMode: PageTurnMode = PageTurnMode.Scroll,
    val autoScrollEnabled: Boolean = false,
    val autoScrollSpeed: Float = 1.0f,   // lines per second
    val pdfCropMargins: Boolean = false,
    val pdfHighContrast: Boolean = false,
    val pdfFillMode: PdfFillMode = PdfFillMode.FitPage,
    val showPageNumber: Boolean = true,
    val showNavigationButtons: Boolean = false,
    val sliderTimeout: SliderTimeout = SliderTimeout.Medium,
    // TTS
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsEngine: String = "SYSTEM",
    val ttsVoiceId: String = "",
    // E-ink
    val eInkMode: Boolean = false,
    // Security
    val biometricLockEnabled: Boolean = false,
    val pdfRequireAuth: Boolean = false,
    val allowScreenshots: Boolean = false,
    // Storage
    val autoClearCache: Boolean = false,
)
