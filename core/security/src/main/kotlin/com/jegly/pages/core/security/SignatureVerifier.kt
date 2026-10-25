package com.jegly.pages.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repackaging / tamper detection. Reads the certificate the APK is currently signed
 * with and compares it against the pinned release certificate.
 *
 * If an attacker decompiles, modifies, and rebuilds the app they must re-sign it with
 * their own key — the certificate digest then no longer matches.
 */
@Singleton
class SignatureVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** True only if the running APK's current signer is the pinned release certificate. */
    fun isSignedByTrustedCert(): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        // Current signer only — never signingCertificateHistory, which still contains key0.
        val signers = info.signingInfo?.apkContentsSigners ?: return false
        val md = MessageDigest.getInstance("SHA-256")
        val want = expected()
        signers.any { md.digest(it.toByteArray()).contentEquals(want) }
    }.getOrDefault(false)

    /** Reconstruct the expected 32-byte cert digest from the masked form (never materialized as hex). */
    private fun expected(): ByteArray {
        val masked = if (Build.VERSION.SDK_INT >= 37) P else M
        return ByteArray(masked.size) { (masked[it] xor K[it % K.size]).toByte() }
    }

    private companion object {
        // Expected signing-cert SHA-256 digests, stored XOR-masked (never as greppable hex —
        // string literals survive R8) and compared as raw bytes. M = jegly key0 (98d324d4…),
        // P = ML-DSA-87 PQC key (5f99de5b…).
        // API 37+ verifies the APK Signature Scheme v3.2 hybrid block, so the current signer is
        // the PQC key there. key0 is deliberately NOT accepted on API 37+: an APK re-signed with
        // only key0 would otherwise pass. Older releases ignore v3.2 and still see key0.
        val K = intArrayOf(0x2D, 0x4D, 0xBA, 0x37, 0xA3, 0x4C, 0x83, 0xDA, 0xA5)
        val M = intArrayOf(
            0xB5, 0x9E, 0x9E, 0xE3, 0xB3, 0x26, 0xB5, 0x56, 0xC7, 0x5F, 0xD7, 0xB0, 0x13, 0x7A, 0xE0, 0x19,
            0xB1, 0xE2, 0xD5, 0xE1, 0xF6, 0x52, 0x26, 0x78, 0x06, 0xEB, 0x55, 0xC3, 0x03, 0x0C, 0x97, 0xEF,
        )
        val P = intArrayOf(
            0x72, 0xD4, 0x64, 0x6C, 0x92, 0xD9, 0xF6, 0xE7, 0xEA, 0xC5, 0x90, 0x11, 0xDC, 0x71, 0xB7, 0xA4,
            0xEF, 0xCE, 0x0C, 0x44, 0x6C, 0xCC, 0x11, 0x0B, 0x9C, 0x11, 0x18, 0xDF, 0x57, 0x24, 0x41, 0x05,
        )
    }
}
