package com.zephyrus.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for SSH private keys using Android Keystore.
 * Keys are encrypted at rest using AES-256-GCM.
 */
class SecureKeyStore(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Store an SSH private key securely.
     */
    fun storePrivateKey(alias: String, key: ByteArray) {
        prefs.edit()
            .putString(keyAlias(alias), key.encodeToBase64())
            .apply()
    }
    
    /**
     * Retrieve an SSH private key.
     */
    fun getPrivateKey(alias: String): ByteArray? {
        return prefs.getString(keyAlias(alias), null)?.decodeFromBase64()
    }
    
    /**
     * Delete an SSH private key.
     */
    fun deletePrivateKey(alias: String) {
        prefs.edit()
            .remove(keyAlias(alias))
            .apply()
    }
    
    /**
     * Check if a key exists.
     */
    fun hasKey(alias: String): Boolean {
        return prefs.contains(keyAlias(alias))
    }
    
    /**
     * Get all stored key aliases.
     */
    fun getAllKeyAliases(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }
    }
    
    private fun keyAlias(alias: String) = "$KEY_PREFIX$alias"
    
    private fun ByteArray.encodeToBase64(): String =
        android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
    
    private fun String.decodeFromBase64(): ByteArray =
        android.util.Base64.decode(this, android.util.Base64.NO_WRAP)
    
    companion object {
        private const val PREFS_NAME = "zephyrus_secure_keys"
        private const val KEY_PREFIX = "ssh_key_"
    }
}
