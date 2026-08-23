package com.jegly.pages.core.datastore

import android.content.SharedPreferences
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.FontFamily
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.PdfFillMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.SliderTimeout
import com.jegly.pages.core.model.UserPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PagesPreferencesDataSource @Inject constructor(
    private val prefs: SharedPreferences,
) {

    private fun observePrefs(): Flow<SharedPreferences> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> trySend(p) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val userPreferences: Flow<UserPreferences> = observePrefs().map { p ->
        UserPreferences(
            accentColor = p.getString(ACCENT_COLOR, null)
                ?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
                ?: AccentColor.Sapphire,
            fontScaleFactor = p.getFloat(FONT_SCALE, 1.0f),
            readingTheme = p.getString(READING_THEME, null)
                ?.let { runCatching { ReadingTheme.valueOf(it) }.getOrNull() }
                ?: ReadingTheme.System,
            useDynamicColor = p.getBoolean(USE_DYNAMIC_COLOR, false),
            lastOpenedBookUri = p.getString(LAST_OPENED_BOOK_URI, null),
            lineSpacingMultiplier = p.getFloat(LINE_SPACING, 1.4f),
            horizontalMarginDp = p.getInt(HORIZONTAL_MARGIN, 16),
            fontFamily = p.getString(FONT_FAMILY, null)
                ?.let { runCatching { FontFamily.valueOf(it) }.getOrNull() }
                ?: FontFamily.Default,
            brightness = p.getFloat(BRIGHTNESS, -1f),
            blueLightFilter = p.getBoolean(BLUE_LIGHT_FILTER, false),
            customBackgroundColor = if (p.contains(CUSTOM_BG_COLOR)) p.getLong(CUSTOM_BG_COLOR, 0L) else null,
            pageTurnMode = p.getString(PAGE_TURN_MODE, null)
                ?.let { runCatching { PageTurnMode.valueOf(it) }.getOrNull() }
                ?: PageTurnMode.Scroll,
            autoScrollEnabled = p.getBoolean(AUTO_SCROLL, false),
            autoScrollSpeed = p.getFloat(AUTO_SCROLL_SPEED, 1.0f),
            pdfCropMargins = p.getBoolean(PDF_CROP_MARGINS, false),
            pdfHighContrast = p.getBoolean(PDF_HIGH_CONTRAST, false),
            pdfFillMode = p.getString(PDF_FILL_MODE, null)
                ?.let { runCatching { PdfFillMode.valueOf(it) }.getOrNull() }
                ?: PdfFillMode.FitPage,
            showPageNumber = p.getBoolean(SHOW_PAGE_NUMBER, true),
            showNavigationButtons = p.getBoolean(SHOW_NAV_BUTTONS, false),
            sliderTimeout = p.getString(SLIDER_TIMEOUT, null)
                ?.let { runCatching { SliderTimeout.valueOf(it) }.getOrNull() }
                ?: SliderTimeout.Medium,
            ttsSpeed = p.getFloat(TTS_SPEED, 1.0f),
            ttsPitch = p.getFloat(TTS_PITCH, 1.0f),
            ttsEngine = p.getString(TTS_ENGINE, "SYSTEM") ?: "SYSTEM",
            ttsVoiceId = p.getString(TTS_VOICE_ID, "") ?: "",
            eInkMode = p.getBoolean(EINK_MODE, false),
            biometricLockEnabled = p.getBoolean(BIOMETRIC_LOCK, false),
            pdfRequireAuth = p.getBoolean(PDF_REQUIRE_AUTH, false),
            allowScreenshots = p.getBoolean(ALLOW_SCREENSHOTS, false),
            autoClearCache = p.getBoolean(AUTO_CLEAR_CACHE, false),
        )
    }

    fun setAccentColor(color: AccentColor) = prefs.edit().putString(ACCENT_COLOR, color.name).apply()
    fun setFontScaleFactor(scale: Float) = prefs.edit().putFloat(FONT_SCALE, scale).apply()
    fun setReadingTheme(theme: ReadingTheme) = prefs.edit().putString(READING_THEME, theme.name).apply()
    fun setUseDynamicColor(use: Boolean) = prefs.edit().putBoolean(USE_DYNAMIC_COLOR, use).apply()
    fun setLastOpenedBookUri(uri: String?) = prefs.edit().run {
        if (uri != null) putString(LAST_OPENED_BOOK_URI, uri) else remove(LAST_OPENED_BOOK_URI)
    }.apply()
    fun setLineSpacing(multiplier: Float) = prefs.edit().putFloat(LINE_SPACING, multiplier).apply()
    fun setHorizontalMargin(dp: Int) = prefs.edit().putInt(HORIZONTAL_MARGIN, dp).apply()
    fun setFontFamily(family: FontFamily) = prefs.edit().putString(FONT_FAMILY, family.name).apply()
    fun setBrightness(value: Float) = prefs.edit().putFloat(BRIGHTNESS, value).apply()
    fun setBlueLightFilter(enabled: Boolean) = prefs.edit().putBoolean(BLUE_LIGHT_FILTER, enabled).apply()
    fun setCustomBackgroundColor(argb: Long?) = prefs.edit().run {
        if (argb != null) putLong(CUSTOM_BG_COLOR, argb) else remove(CUSTOM_BG_COLOR)
    }.apply()
    fun setPageTurnMode(mode: PageTurnMode) = prefs.edit().putString(PAGE_TURN_MODE, mode.name).apply()
    fun setAutoScroll(enabled: Boolean) = prefs.edit().putBoolean(AUTO_SCROLL, enabled).apply()
    fun setAutoScrollSpeed(speed: Float) = prefs.edit().putFloat(AUTO_SCROLL_SPEED, speed).apply()
    fun setPdfCropMargins(enabled: Boolean) = prefs.edit().putBoolean(PDF_CROP_MARGINS, enabled).apply()
    fun setPdfHighContrast(enabled: Boolean) = prefs.edit().putBoolean(PDF_HIGH_CONTRAST, enabled).apply()
    fun setPdfFillMode(mode: PdfFillMode) = prefs.edit().putString(PDF_FILL_MODE, mode.name).apply()
    fun setShowPageNumber(enabled: Boolean) = prefs.edit().putBoolean(SHOW_PAGE_NUMBER, enabled).apply()
    fun setShowNavigationButtons(enabled: Boolean) = prefs.edit().putBoolean(SHOW_NAV_BUTTONS, enabled).apply()
    fun setTtsSpeed(speed: Float) = prefs.edit().putFloat(TTS_SPEED, speed).apply()
    fun setTtsPitch(pitch: Float) = prefs.edit().putFloat(TTS_PITCH, pitch).apply()
    fun setTtsEngine(engine: String) = prefs.edit().putString(TTS_ENGINE, engine).apply()
    fun setTtsVoiceId(voiceId: String) = prefs.edit().putString(TTS_VOICE_ID, voiceId).apply()
    fun setEInkMode(enabled: Boolean) = prefs.edit().putBoolean(EINK_MODE, enabled).apply()
    fun setBiometricLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(BIOMETRIC_LOCK, enabled).apply()
    fun setPdfRequireAuth(enabled: Boolean) = prefs.edit().putBoolean(PDF_REQUIRE_AUTH, enabled).apply()
    fun setAllowScreenshots(enabled: Boolean) = prefs.edit().putBoolean(ALLOW_SCREENSHOTS, enabled).apply()
    fun setAutoClearCache(enabled: Boolean) = prefs.edit().putBoolean(AUTO_CLEAR_CACHE, enabled).apply()
    fun setSliderTimeout(timeout: SliderTimeout) = prefs.edit().putString(SLIDER_TIMEOUT, timeout.name).apply()

    companion object {
        private const val ACCENT_COLOR = "accent_color"
        private const val FONT_SCALE = "font_scale"
        private const val READING_THEME = "reading_theme"
        private const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        private const val LAST_OPENED_BOOK_URI = "last_opened_book_uri"
        private const val LINE_SPACING = "line_spacing"
        private const val HORIZONTAL_MARGIN = "horizontal_margin"
        private const val FONT_FAMILY = "font_family"
        private const val BRIGHTNESS = "brightness"
        private const val BLUE_LIGHT_FILTER = "blue_light_filter"
        private const val CUSTOM_BG_COLOR = "custom_bg_color"
        private const val PAGE_TURN_MODE = "page_turn_mode"
        private const val AUTO_SCROLL = "auto_scroll"
        private const val AUTO_SCROLL_SPEED = "auto_scroll_speed"
        private const val PDF_CROP_MARGINS = "pdf_crop_margins"
        private const val PDF_HIGH_CONTRAST = "pdf_high_contrast"
        private const val PDF_FILL_MODE = "pdf_fill_mode"
        private const val SHOW_PAGE_NUMBER = "show_page_number"
        private const val SHOW_NAV_BUTTONS = "show_nav_buttons"
        private const val TTS_SPEED = "tts_speed"
        private const val TTS_PITCH = "tts_pitch"
        private const val TTS_ENGINE = "tts_engine"
        private const val TTS_VOICE_ID = "tts_voice_id"
        private const val EINK_MODE = "eink_mode"
        private const val BIOMETRIC_LOCK = "biometric_lock"
        private const val PDF_REQUIRE_AUTH = "pdf_require_auth"
        private const val ALLOW_SCREENSHOTS = "allow_screenshots"
        private const val AUTO_CLEAR_CACHE = "auto_clear_cache"
        private const val SLIDER_TIMEOUT = "slider_timeout"
    }
}
