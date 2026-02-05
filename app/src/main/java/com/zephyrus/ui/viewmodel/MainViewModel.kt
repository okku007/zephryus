package com.zephyrus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.data.ConnectionState
import com.zephyrus.data.OperationState
import com.zephyrus.data.ServerConfigRepository
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
    private val configRepo: ServerConfigRepository
) : ViewModel() {
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    private val _containerStatus = MutableStateFlow<String?>(null)
    val containerStatus: StateFlow<String?> = _containerStatus.asStateFlow()
    
    private val _connectionLog = MutableStateFlow<List<String>>(emptyList())
    val connectionLog: StateFlow<List<String>> = _connectionLog.asStateFlow()
    
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
    
    fun connect() {
        viewModelScope.launch {
            clearLog()
            _connectionState.value = ConnectionState.Connecting
            
            log("Starting connection to ${configRepo.host}:${configRepo.port}")
            log("Username: ${configRepo.username}")
            
            val privateKey = keyStore.getPrivateKey(configRepo.keyAlias)
            if (privateKey == null) {
                log("ERROR: No SSH key found in secure storage")
                _connectionState.value = ConnectionState.Error("No SSH key imported")
                return@launch
            }
            
            log("SSH key loaded (${privateKey.size} bytes)")
            log("Key preview: ${String(privateKey.take(50).toByteArray())}...")
            
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
                    log("ERROR: ${e.javaClass.simpleName}")
                    log("Message: ${e.message}")
                    e.cause?.let { cause ->
                        log("Cause: ${cause.javaClass.simpleName}: ${cause.message}")
                    }
                    // Log stack trace
                    e.stackTrace.take(5).forEach { frame ->
                        log("  at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
                    }
                    _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
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
        }
    }
    
    fun stopContainer() {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress
            
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
        }
    }
    
    fun startContainer() {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress
            
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
        }
    }
    
    fun refreshContainerStatus() {
        viewModelScope.launch {
            val command = DockerCommands.status(configRepo.containerName)
            sshClient.executeCommand(command).fold(
                onSuccess = { output ->
                    _containerStatus.value = output.trim().ifEmpty { "Not found" }
                },
                onFailure = {
                    _containerStatus.value = "Unknown"
                }
            )
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
