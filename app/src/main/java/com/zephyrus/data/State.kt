package com.zephyrus.data

/**
 * Represents the connection state of the SSH client.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Represents the state of a Docker operation.
 */
sealed class OperationState {
    data object Idle : OperationState()
    data object InProgress : OperationState()
    data class Success(val output: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
