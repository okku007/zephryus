package com.zephyrus.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.data.ConnectionState
import com.zephyrus.data.OperationState
import com.zephyrus.data.ServerConfigRepository
import com.zephyrus.security.HostKeyStore
import com.zephyrus.security.SecureKeyStore
import com.zephyrus.ssh.DockerCommands
import com.zephyrus.ssh.SshClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val sshClient: SshClient,
    private val keyStore: SecureKeyStore,
    private val configRepo: ServerConfigRepository,
    private val hostKeyStore: HostKeyStore
) : ViewModel() {
    
    companion object {
        private const val TAG = "Zephyrus"
    }
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    private val _containerStatus = MutableStateFlow<String?>(null)
    val containerStatus: StateFlow<String?> = _containerStatus.asStateFlow()
    
    private val _connectionLog = MutableStateFlow<List<String>>(emptyList())
    val connectionLog: StateFlow<List<String>> = _connectionLog.asStateFlow()
    
    // Host key verification states
    private val _pendingHostKey = MutableStateFlow<PendingHostKey?>(null)
    val pendingHostKey: StateFlow<PendingHostKey?> = _pendingHostKey.asStateFlow()
    
    data class PendingHostKey(
        val host: String,
        val port: Int,
        val fingerprint: String,
        val isChanged: Boolean,
        val oldFingerprint: String? = null
    )
    
    init {
        // Setup host key verification callbacks
        sshClient.onNewHostKey = { host, port, fingerprint ->
            // For now, auto-accept on first connection with logging
            log("New host verified: $host:$port")
            log("Fingerprint: ${fingerprint.take(32)}...")
            true  // Auto-accept first-time connections
        }
        
        sshClient.onHostKeyChanged = { host, port, oldFingerprint, newFingerprint ->
            // SECURITY: Reject changed host keys (possible MITM attack)
            log("⚠️ WARNING: Host key changed for $host:$port!")
            log("This could indicate a man-in-the-middle attack.")
            Log.w(TAG, "Host key changed for $host:$port - old: $oldFingerprint, new: $newFingerprint")
            false  // Reject by default for security
        }
    }
    
    private fun log(message: String) {
        _connectionLog.value = _connectionLog.value + "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $message"
    }
    
    fun clearLog() {
        _connectionLog.value = emptyList()
    }
    
    // Configuration properties
    val host get() = configRepo.host
    val port get() = configRepo.port
    val username get() = configRepo.username
    val containerName get() = configRepo.containerName
    val hasPrivateKey get() = keyStore.hasKey(configRepo.keyAlias)
    
    fun updateHost(value: String) { configRepo.host = value }
    fun updatePort(value: Int) { configRepo.port = value }
    fun updateUsername(value: String) { configRepo.username = value }
    fun updateContainerName(value: String) { configRepo.containerName = value }
    
    fun importPrivateKey(keyContent: ByteArray) {
        keyStore.storePrivateKey(configRepo.keyAlias, keyContent)
    }
    
    /**
     * Clear stored host key (use when server key has legitimately changed).
     */
    fun clearStoredHostKey() {
        hostKeyStore.removeFingerprint(configRepo.host, configRepo.port)
        log("Cleared stored host key for ${configRepo.host}:${configRepo.port}")
    }
    
    fun connect() {
        viewModelScope.launch {
            clearLog()
            _connectionState.value = ConnectionState.Connecting
            
            // Validate configuration before connecting
            if (configRepo.host.isBlank()) {
                log("ERROR: Host address is required")
                _connectionState.value = ConnectionState.Error("Host address not configured")
                return@launch
            }
            
            if (configRepo.username.isBlank()) {
                log("ERROR: Username is required")
                _connectionState.value = ConnectionState.Error("Username not configured")
                return@launch
            }
            
            log("Connecting to ${configRepo.host}:${configRepo.port}")
            log("Username: ${configRepo.username}")
            
            val privateKey = keyStore.getPrivateKey(configRepo.keyAlias)
            if (privateKey == null) {
                log("ERROR: No SSH key found in secure storage")
                _connectionState.value = ConnectionState.Error("No SSH key imported")
                return@launch
            }
            
            log("SSH key loaded successfully")
            
            val passphrase = configRepo.keyPassphrase.ifBlank { null }
            if (passphrase != null) {
                log("Using passphrase for key decryption")
            }
            
            log("Initiating SSH handshake...")
            
            sshClient.connect(
                host = configRepo.host,
                port = configRepo.port,
                username = configRepo.username,
                privateKey = privateKey,
                passphrase = passphrase
            ).fold(
                onSuccess = {
                    log("SUCCESS: Connected!")
                    _connectionState.value = ConnectionState.Connected
                    refreshContainerStatus()
                },
                onFailure = { e ->
                    // VULN-007 FIX: Minimal error info in UI, detailed logs to system
                    val userMessage = when {
                        e.message?.contains("Auth") == true -> "Authentication failed. Check your SSH key."
                        e.message?.contains("Connection refused") == true -> "Connection refused. Check host and port."
                        e.message?.contains("timed out") == true -> "Connection timed out. Check network."
                        e.message?.contains("Host key") == true -> "Host key verification failed."
                        else -> "Connection failed. Check settings and try again."
                    }
                    log("ERROR: $userMessage")
                    // Log full details to Android system log only (not UI)
                    Log.e(TAG, "SSH connection error", e)
                    _connectionState.value = ConnectionState.Error(userMessage)
                }
            )
        }
    }
    
    fun disconnect() {
        sshClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _containerStatus.value = null
    }
    
    fun restartContainer() {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress
            
            try {
                val command = DockerCommands.restart(configRepo.containerName)
                sshClient.executeCommand(command).fold(
                    onSuccess = { output ->
                        _operationState.value = OperationState.Success(output)
                        refreshContainerStatus()
                    },
                    onFailure = { e ->
                        _operationState.value = OperationState.Error(e.message ?: "Restart failed")
                    }
                )
            } catch (e: IllegalArgumentException) {
                _operationState.value = OperationState.Error("Invalid container name")
            }
        }
    }
    
    fun stopContainer() {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress
            
            try {
                val command = DockerCommands.stop(configRepo.containerName)
                sshClient.executeCommand(command).fold(
                    onSuccess = { output ->
                        _operationState.value = OperationState.Success(output)
                        refreshContainerStatus()
                    },
                    onFailure = { e ->
                        _operationState.value = OperationState.Error(e.message ?: "Stop failed")
                    }
                )
            } catch (e: IllegalArgumentException) {
                _operationState.value = OperationState.Error("Invalid container name")
            }
        }
    }
    
    fun startContainer() {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress
            
            try {
                val command = DockerCommands.start(configRepo.containerName)
                sshClient.executeCommand(command).fold(
                    onSuccess = { output ->
                        _operationState.value = OperationState.Success(output)
                        refreshContainerStatus()
                    },
                    onFailure = { e ->
                        _operationState.value = OperationState.Error(e.message ?: "Start failed")
                    }
                )
            } catch (e: IllegalArgumentException) {
                _operationState.value = OperationState.Error("Invalid container name")
            }
        }
    }
    
    fun refreshContainerStatus() {
        viewModelScope.launch {
            try {
                val command = DockerCommands.status(configRepo.containerName)
                sshClient.executeCommand(command).fold(
                    onSuccess = { output ->
                        _containerStatus.value = output.trim().ifEmpty { "Not found" }
                    },
                    onFailure = {
                        _containerStatus.value = "Unknown"
                    }
                )
            } catch (e: IllegalArgumentException) {
                _containerStatus.value = "Invalid container name"
            }
        }
    }
    
    fun clearOperationState() {
        _operationState.value = OperationState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        sshClient.disconnect()
    }
}
