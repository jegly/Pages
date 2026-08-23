package com.jegly.pages.core.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "pages_book_enc_v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val BOOKS_DIR = "books"
        const val ENCRYPTED_EXT = ".enc"
    }

    private val booksDir = File(context.filesDir, BOOKS_DIR).also { it.mkdirs() }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        ks.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)  // Keystore generates a fresh random IV per call
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .also { it.init(spec) }
            .generateKey()
    }

    // File format: [12-byte IV][ciphertext][16-byte GCM auth tag]
    // Using doFinal() on both sides — CipherInputStream silently swallows AEADBadTagException,
    // so decryption always buffers fully to guarantee the auth tag is verified before use.
    suspend fun encryptFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val plaintext = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return@runCatching null

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv                      // 12 bytes, Keystore-generated random
            val ciphertext = cipher.doFinal(plaintext)  // ciphertext + 16-byte GCM tag appended

            val outFile = File(booksDir, "${System.currentTimeMillis()}$ENCRYPTED_EXT")
            FileOutputStream(outFile).use { fos ->
                fos.write(iv)
                fos.write(ciphertext)
            }
            outFile
        }.getOrNull()
    }

    suspend fun decryptToTempFile(encryptedFile: File): File = withContext(Dispatchers.IO) {
        val raw = encryptedFile.readBytes()
        val iv = raw.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = raw.copyOfRange(GCM_IV_BYTES, raw.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        // doFinal throws AEADBadTagException immediately if the file was tampered with.
        val plaintext = cipher.doFinal(ciphertext)

        val tempFile = File(context.cacheDir, "${encryptedFile.nameWithoutExtension}_dec")
        tempFile.writeBytes(plaintext)
        tempFile
    }

    fun deleteEncryptedBook(absolutePath: String) {
        File(absolutePath).takeIf { it.exists() && it.startsWith(booksDir) }?.delete()
    }
}
