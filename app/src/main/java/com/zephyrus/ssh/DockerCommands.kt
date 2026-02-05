package com.zephyrus.ssh

/**
 * Docker command builder for remote execution.
 * Uses absolute paths to work in non-interactive shells.
 */
object DockerCommands {
    private const val DOCKER_BIN = "/usr/bin/docker"
    
    /**
     * Restart a Docker container.
     */
    fun restart(containerName: String, timeout: Int = 10): String =
        "$DOCKER_BIN restart -t $timeout $containerName"
    
    /**
     * Get container status.
     */
    fun status(containerName: String): String =
        "$DOCKER_BIN ps -f name=$containerName --format '{{.Status}}'"
    
    /**
     * Check if container is running.
     */
    fun isRunning(containerName: String): String =
        "$DOCKER_BIN inspect -f '{{.State.Running}}' $containerName"
    
    /**
     * Stop a container.
     */
    fun stop(containerName: String, timeout: Int = 10): String =
        "$DOCKER_BIN stop -t $timeout $containerName"
    
    /**
     * Start a container.
     */
    fun start(containerName: String): String =
        "$DOCKER_BIN start $containerName"
    
    /**
     * Get container logs (last N lines).
     */
    fun logs(containerName: String, lines: Int = 50): String =
        "$DOCKER_BIN logs --tail $lines $containerName"
}
