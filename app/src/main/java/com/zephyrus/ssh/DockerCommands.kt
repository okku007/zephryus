package com.zephyrus.ssh

/**
 * Docker command builder for remote execution.
 * Uses absolute paths to work in non-interactive shells.
 * All inputs are sanitized to prevent command injection.
 */
object DockerCommands {
    private const val DOCKER_BIN = "/usr/bin/docker"
    
    // Docker container names must start with alphanumeric and contain only [a-zA-Z0-9_.-]
    private val SAFE_NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$")
    
    /**
     * Validates and returns a safe container name.
     * @throws IllegalArgumentException if name is invalid
     */
    private fun sanitize(containerName: String): String {
        require(containerName.isNotBlank()) { "Container name cannot be empty" }
        require(SAFE_NAME_REGEX.matches(containerName)) { 
            "Invalid container name: must start with alphanumeric and contain only [a-zA-Z0-9_.-]" 
        }
        return containerName
    }
    
    /**
     * Restart a Docker container.
     */
    fun restart(containerName: String, timeout: Int = 10): String {
        val safeName = sanitize(containerName)
        val safeTimeout = timeout.coerceIn(1, 300)
        return "$DOCKER_BIN restart -t $safeTimeout $safeName"
    }
    
    /**
     * Get container status.
     */
    fun status(containerName: String): String {
        val safeName = sanitize(containerName)
        return "$DOCKER_BIN ps -f name=$safeName --format '{{.Status}}'"
    }
    
    /**
     * Check if container is running.
     */
    fun isRunning(containerName: String): String {
        val safeName = sanitize(containerName)
        return "$DOCKER_BIN inspect -f '{{.State.Running}}' $safeName"
    }
    
    /**
     * Stop a container.
     */
    fun stop(containerName: String, timeout: Int = 10): String {
        val safeName = sanitize(containerName)
        val safeTimeout = timeout.coerceIn(1, 300)
        return "$DOCKER_BIN stop -t $safeTimeout $safeName"
    }
    
    /**
     * Start a container.
     */
    fun start(containerName: String): String {
        val safeName = sanitize(containerName)
        return "$DOCKER_BIN start $safeName"
    }
    
    /**
     * Get container logs (last N lines).
     */
    fun logs(containerName: String, lines: Int = 50): String {
        val safeName = sanitize(containerName)
        val safeLines = lines.coerceIn(1, 1000)
        return "$DOCKER_BIN logs --tail $safeLines $safeName"
    }
}
