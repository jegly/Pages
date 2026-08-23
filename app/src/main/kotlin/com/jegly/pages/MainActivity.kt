package com.jegly.pages

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.jegly.pages.core.designsystem.theme.PagesTheme
import com.jegly.pages.core.model.AccentColor
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.security.SignatureVerifier
import com.jegly.pages.ui.PagesApp
import com.jegly.pages.ui.rememberPagesAppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val accentColorMap = mapOf(
    AccentColor.Emerald to Color(0xFF2D9A63),
    AccentColor.Amber to Color(0xFFD97706),
    AccentColor.Sapphire to Color(0xFF2563EB),
    AccentColor.Ruby to Color(0xFFDC2626),
    AccentColor.Amethyst to Color(0xFF9333EA),
    AccentColor.Rose to Color(0xFFE11D48),
)

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var isAuthenticated by mutableStateOf(false)
    private val mainViewModel: MainViewModel by viewModels()

    @Inject lateinit var signatureVerifier: SignatureVerifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()

        // Repackaging detection: a re-signed (tampered) build fails this and never renders.
        // Skipped for debug builds, which are signed with the local debug keystore.
        if (!BuildConfig.DEBUG && !signatureVerifier.isSignedByTrustedCert()) {
            showTamperDialog()
            return
        }

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val prefs = mainViewModel.persistedPreferences.first()
                if (prefs.biometricLockEnabled) checkAndAuthenticate() else isAuthenticated = true
            }
        } else {
            isAuthenticated = savedInstanceState.getBoolean(KEY_IS_AUTHENTICATED, false)
        }

        setContent {
            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (prefs.readingTheme) {
                ReadingTheme.Light -> false
                ReadingTheme.Dark -> true
                else -> systemDark
            }

            PagesTheme(
                darkTheme = darkTheme,
                dynamicColor = prefs.useDynamicColor,
                eInkMode = prefs.eInkMode,
                seedColor = accentColorMap[prefs.accentColor],
            ) {
                SideEffect {
                    if (prefs.allowScreenshots) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
                if (isAuthenticated) {
                    val appState = rememberPagesAppState()
                    PagesApp(appState = appState)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_AUTHENTICATED, isAuthenticated)
    }

    override fun onStop() {
        super.onStop()
        getSystemService(ClipboardManager::class.java)?.clearPrimaryClip()
        if (mainViewModel.preferences.value.autoClearCache) {
            cacheDir.deleteRecursively()
            externalCacheDir?.deleteRecursively()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated) {
            lifecycleScope.launch {
                val prefs = mainViewModel.persistedPreferences.first()
                if (prefs.biometricLockEnabled) checkAndAuthenticate() else isAuthenticated = true
            }
        }
    }

    private fun checkAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showBiometricPrompt()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> showNoDeviceLockDialog()
            else -> showNoDeviceLockDialog()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .setConfirmationRequired(true)
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        finish()
                    }
                }

                override fun onAuthenticationFailed() = Unit
            },
        )
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showTamperDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tamper_detected_title))
            .setMessage(getString(R.string.tamper_detected_message))
            .setPositiveButton(getString(R.string.exit)) { _, _ -> finish() }
            .setCancelable(false)
            .setOnDismissListener { finish() }
            .show()
    }

    private fun showNoDeviceLockDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.no_device_lock_title))
            .setMessage(getString(R.string.no_device_lock_message))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                finish()
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    }
}
