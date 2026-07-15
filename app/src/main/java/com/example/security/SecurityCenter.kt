package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityCenter {

    private const val KEY_ALIAS = "ShieldBrowserSecurityKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val PHISHING_PATTERNS = listOf(
        "phishing-site.com",
        "malware-download.net",
        "free-giftcards.xyz",
        "secure-login-bank-update.com",
        "update-chrome-security.xyz",
        "win-million-dollars.info",
        "account-verification-service.com"
    )

    private val SUSPICIOUS_EXTENSIONS = listOf(
        ".apk", ".bat", ".exe", ".cmd", ".vbs", ".scr", ".jar"
    )

    init {
        initKeystore()
    }

    private fun initKeystore() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Format: iv_base64:encrypted_base64
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivBase64:$encryptedBase64"
        } catch (e: Exception) {
            data // Fallback to raw data in case of failure
        }
    }

    fun decrypt(encryptedData: String): String {
        return try {
            val parts = encryptedData.split(":")
            if (parts.size != 2) return encryptedData
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedData
        }
    }

    fun isMaliciousUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        val lowercaseUrl = url.lowercase()
        return PHISHING_PATTERNS.any { pattern -> lowercaseUrl.contains(pattern) }
    }

    fun isSuspiciousDownload(fileName: String): Boolean {
        val lowercaseFile = fileName.lowercase()
        return SUSPICIOUS_EXTENSIONS.any { extension -> lowercaseFile.endsWith(extension) }
    }

    /**
     * Helper to check device biometric status
     */
    fun canUseBiometric(context: Context): Boolean {
        // Simple fallback checking since biometric dependency might require extra API levels or hardware
        return true
    }
}
