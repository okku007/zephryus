package com.zephyrus.ssh

import android.os.Build
import androidx.annotation.RequiresApi
import com.zephyrus.security.HostKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.StringReader
import java.security.PublicKey
import java.util.concurrent.TimeUnit

/**
 * SSH client wrapper using SSHJ for secure shell connections.
 * Supports OpenSSH private key format, Ed25519, RSA, and ECDSA keys.
 * 
 * Security features:
 * - Host key verification to prevent MITM attacks
 * - In-memory key loading (no temp files)
 */
class SshClient(private val hostKeyStore: HostKeyStore) {
    private var client: SSHClient? = null
    
    // Callback for when a new host is encountered
    var onNewHostKey: ((host: String, port: Int, fingerprint: String) -> Boolean)? = null
    
    // Callback for when host key has changed (possible attack!)
    var onHostKeyChanged: ((host: String, port: Int, oldFingerprint: String, newFingerprint: String) -> Boolean)? = null
    
    /**
     * Host key verifier that checks against stored fingerprints.
     */
    private inner class StoredHostKeyVerifier(
        private val targetHost: String,
        private val targetPort: Int
    ) : HostKeyVerifier {
        
        override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
            val fingerprint = SecurityUtils.getFingerprint(key)
            val storedFingerprint = hostKeyStore.getFingerprint(targetHost, targetPort)
            
            return when {
                // First connection - ask user to verify
                storedFingerprint == null -> {
                    val accepted = onNewHostKey?.invoke(targetHost, targetPort, fingerprint) ?: false
                    if (accepted) {
                        hostKeyStore.storeFingerprint(targetHost, targetPort, fingerprint)
                    }
                    accepted
                }
                // Fingerprint matches - safe
                storedFingerprint == fingerprint -> true
                // Fingerprint changed - possible MITM attack!
                else -> {
                    onHostKeyChanged?.invoke(targetHost, targetPort, storedFingerprint, fingerprint) ?: false
                }
            }
        }
        
        override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> {
            // Return empty list - we don't restrict algorithms, just verify fingerprints
            return mutableListOf()
        }
    }
    
    /**
     * Connect to an SSH server using private key authentication.
     */
    suspend fun connect(
        host: String,
        port: Int = 22,
        username: String,
        privateKey: ByteArray,
        passphrase: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Disconnect any existing connection
            disconnect()
            
            // Create new SSH client
            val ssh = SSHClient()
            
            // Use secure host key verification (VULN-002 fix)
            ssh.addHostKeyVerifier(StoredHostKeyVerifier(host, port))
            
            // Set connection timeout
            ssh.connectTimeout = 30_000
            ssh.timeout = 30_000
            
            // Connect to server
            ssh.connect(host, port)
            
            // Convert key bytes to string and normalize line endings
            var keyString = String(privateKey, Charsets.UTF_8)
            // Normalize line endings to Unix-style (LF only)
            keyString = keyString.replace("\r\n", "\n").replace("\r", "\n")
            // Ensure the key ends with a newline
            if (!keyString.endsWith("\n")) {
                keyString += "\n"
            }
            
            // VULN-003 FIX: Load key directly from memory using StringReader
            // instead of writing to temp file
            val keyProvider: KeyProvider = if (passphrase != null && passphrase.isNotEmpty()) {
                ssh.loadKeys(keyString, null, net.schmizz.sshj.userauth.password.PasswordUtils.createOneOff(passphrase.toCharArray()))
            } else {
                ssh.loadKeys(keyString, null, null)
            }
            
            // Authenticate with private key
            ssh.authPublickey(username, keyProvider)
            
            client = ssh
        }
    }
    
    /**
     * Execute a command on the remote server.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ssh = client ?: throw IllegalStateException("Not connected")
            
            val session: Session = ssh.startSession()
            try {
                val cmd = session.exec(command)
                
                // Read output
                val output = IOUtils.readFully(cmd.inputStream).toString(Charsets.UTF_8)
                val error = IOUtils.readFully(cmd.errorStream).toString(Charsets.UTF_8)
                
                // Wait for command to complete
                cmd.join(30, TimeUnit.SECONDS)
                
                val exitStatus = cmd.exitStatus ?: 0
                
                if (exitStatus != 0 && error.isNotEmpty()) {
                    throw RuntimeException("Command failed (exit $exitStatus): $error")
                }
                
                output.ifEmpty { "Command executed successfully (exit code: $exitStatus)" }
            } finally {
                session.close()
            }
        }
    }
    
    /**
     * Check if connected to the server.
     */
    fun isConnected(): Boolean = client?.isConnected == true
    
    /**
     * Disconnect from the server.
     */
    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            // Ignore disconnect errors
        }
        client = null
    }
}
