package com.jegly.pages.core.security

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repackaging / tamper detection. Reads the certificate the APK is currently signed
 * with and compares it against the pinned release certificate.
 *
 * If an attacker decompiles, modifies, and rebuilds the app they must re-sign it with
 * their own key — the certificate digest then no longer matches [EXPECTED_SHA256].
 */
@Singleton
class SignatureVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** True only if the running APK is signed by the pinned release certificate. */
    fun isSignedByTrustedCert(): Boolean =
        signingCertDigests().any { it.equals(EXPECTED_SHA256, ignoreCase = true) }

    private fun signingCertDigests(): List<String> = runCatching {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val signingInfo = info.signingInfo ?: return emptyList()
        val md = MessageDigest.getInstance("SHA-256")
        signingInfo.apkContentsSigners.map { signature ->
            md.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }.getOrDefault(emptyList())

    companion object {
        // SHA-256 of the release signing certificate (keystore alias "key0").
        // Verify with: apksigner verify --print-certs <release-apk>
        private const val EXPECTED_SHA256 =
            "98d324d4106a368c62729a0a24d9ac9a6b47f8ac4c6585348531f0ee4eb6a04c"
    }
}
