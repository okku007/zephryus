package com.zephyrus.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for SSH host key fingerprints.
 * Used to detect MITM attacks by verifying server identity.
 */
class HostKeyStore(context: Context) {
    
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
     * Store a host's key fingerprint.
     */
    fun storeFingerprint(host: String, port: Int, fingerprint: String) {
        prefs.edit()
            .putString(hostKey(host, port), fingerprint)
            .apply()
    }
    
    /**
     * Get stored fingerprint for a host.
     */
    fun getFingerprint(host: String, port: Int): String? {
        return prefs.getString(hostKey(host, port), null)
    }
    
    /**
     * Check if we have a stored fingerprint for a host.
     */
    fun hasFingerprint(host: String, port: Int): Boolean {
        return prefs.contains(hostKey(host, port))
    }
    
    /**
     * Remove a stored fingerprint (for host key rotation).
     */
    fun removeFingerprint(host: String, port: Int) {
        prefs.edit()
            .remove(hostKey(host, port))
            .apply()
    }
    
    /**
     * Get all stored hosts.
     */
    fun getAllHosts(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(HOST_PREFIX) }
            .map { it.removePrefix(HOST_PREFIX) }
    }
    
    private fun hostKey(host: String, port: Int) = "$HOST_PREFIX$host:$port"
    
    companion object {
        private const val PREFS_NAME = "zephyrus_host_keys"
        private const val HOST_PREFIX = "host_"
    }
}
