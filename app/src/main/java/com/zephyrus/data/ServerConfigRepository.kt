package com.zephyrus.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Repository for server connection configuration.
 * Sensitive data (passphrase) is stored using encrypted preferences.
 */
class ServerConfigRepository(context: Context) {
    
    // Regular preferences for non-sensitive configuration
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Encrypted preferences for sensitive data (VULN-004 fix)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    var host: String
        get() = prefs.getString(KEY_HOST, "") ?: ""
        set(value) = prefs.edit { putString(KEY_HOST, value) }
    
    var port: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit { putInt(KEY_PORT, value) }
    
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }
    
    var containerName: String
        get() = prefs.getString(KEY_CONTAINER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CONTAINER, value) }
    
    var keyAlias: String
        get() = prefs.getString(KEY_ALIAS, DEFAULT_KEY_ALIAS) ?: DEFAULT_KEY_ALIAS
        set(value) = prefs.edit { putString(KEY_ALIAS, value) }
    
    // VULN-004 FIX: Passphrase now stored in encrypted preferences
    var keyPassphrase: String
        get() = securePrefs.getString(KEY_PASSPHRASE, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_PASSPHRASE, value).apply()
    
    fun isConfigured(): Boolean {
        return host.isNotBlank() && username.isNotBlank()
    }
    
    fun clear() {
        prefs.edit { clear() }
        securePrefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "server_config"
        private const val SECURE_PREFS_NAME = "server_config_secure"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_CONTAINER = "container"
        private const val KEY_ALIAS = "key_alias"
        private const val KEY_PASSPHRASE = "key_passphrase"
        
        // VULN-006 FIX: Removed hardcoded default host/container
        private const val DEFAULT_PORT = 22
        private const val DEFAULT_KEY_ALIAS = "default"
    }
}
