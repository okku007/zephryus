# Zephyrus

An Android app for managing Docker containers on a remote server via SSH.

## Features

- SSH connection using private key authentication
- Docker container management (start, stop, restart)
- Real-time container status monitoring
- Secure credential storage using Android Keystore

## How It Works

1. The app establishes an SSH connection to your remote server using your private key
2. Once connected, it executes Docker commands over the SSH session
3. Container operations (start/stop/restart) are sent as shell commands
4. The app queries container status using `docker ps` and displays the results
5. All credentials and keys are encrypted locally using Android's Keystore system

## Requirements

- Android 7.0 (API 24) or higher
- SSH access to a server with Docker installed
- User must be in the `docker` group on the server

## Setup

1. Generate an RSA key pair for the app:
   ```
   ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa_zephyrus
   ```

2. Add the public key to your server's `~/.ssh/authorized_keys`

3. Import the private key into the app via Settings

4. Configure the server host, port, username, and container name in Settings

## Tech Stack

- Kotlin
- Jetpack Compose
- SSHJ for SSH connections
- Koin for dependency injection
- AndroidX Security for encrypted storage

## Security

- **SSH Host Key Verification** - Protects against man-in-the-middle attacks
- **Encrypted Credential Storage** - Keys and passphrases stored using AES-256-GCM
- **Input Validation** - Container names validated to prevent command injection
- **No Sensitive Logging** - Private keys are never logged or displayed

## Building

```
./gradlew assembleDebug
```