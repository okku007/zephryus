package com.zephyrus.ssh

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * SSH client wrapper using SSHJ for secure shell connections.
 * Supports OpenSSH private key format, Ed25519, RSA, and ECDSA keys.
 */
class SshClient {
    private var client: SSHClient? = null
    
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
            
            // Accept all host keys (for simplicity - in production, implement proper verification)
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            
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
            
            // Write to temp file
            val tempKeyFile = File.createTempFile("ssh_key_", null, null)
            try {
                tempKeyFile.writeText(keyString, Charsets.UTF_8)
                
                // Use SSHClient.loadKeys() which auto-detects key format
                val keyProvider: KeyProvider = if (passphrase != null && passphrase.isNotEmpty()) {
                    ssh.loadKeys(tempKeyFile.absolutePath, passphrase)
                } else {
                    ssh.loadKeys(tempKeyFile.absolutePath)
                }
                
                // Authenticate with private key
                ssh.authPublickey(username, keyProvider)
                
            } finally {
                // Clean up temp file
                tempKeyFile.delete()
            }
            
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
