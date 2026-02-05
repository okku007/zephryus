# Zephyrus

An Android app for managing Docker containers on a remote server via SSH.

## Features

- SSH connection using private key authentication
- Docker container management (start, stop, restart)
- Real-time container status monitoring
- Secure credential storage using Android Keystore

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

## Building

```
./gradlew assembleDebug
```

## License

MIT
