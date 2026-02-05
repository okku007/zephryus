package com.zephyrus.data

import android.content.Context
import androidx.core.content.edit

/**
 * Repository for server connection configuration.
 */
class ServerConfigRepository(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    var host: String
        get() = prefs.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
        set(value) = prefs.edit { putString(KEY_HOST, value) }
    
    var port: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit { putInt(KEY_PORT, value) }
    
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }
    
    var containerName: String
        get() = prefs.getString(KEY_CONTAINER, DEFAULT_CONTAINER) ?: DEFAULT_CONTAINER
        set(value) = prefs.edit { putString(KEY_CONTAINER, value) }
    
    var keyAlias: String
        get() = prefs.getString(KEY_ALIAS, DEFAULT_KEY_ALIAS) ?: DEFAULT_KEY_ALIAS
        set(value) = prefs.edit { putString(KEY_ALIAS, value) }
    
    var keyPassphrase: String
        get() = prefs.getString(KEY_PASSPHRASE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PASSPHRASE, value) }
    
    fun isConfigured(): Boolean {
        return host.isNotBlank() && username.isNotBlank()
    }
    
    fun clear() {
        prefs.edit { clear() }
    }
    
    companion object {
        private const val PREFS_NAME = "server_config"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_CONTAINER = "container"
        private const val KEY_ALIAS = "key_alias"
        private const val KEY_PASSPHRASE = "key_passphrase"
        
        // Default values for testing
        private const val DEFAULT_HOST = "192.168.2.102"
        private const val DEFAULT_PORT = 22
        private const val DEFAULT_CONTAINER = "blogsite"
        private const val DEFAULT_KEY_ALIAS = "default"
    }
}
