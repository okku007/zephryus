# Add project specific ProGuard rules here.
# Keep JSch classes for SSH functionality
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep SSHJ classes
-keep class net.schmizz.** { *; }
-dontwarn net.schmizz.**
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# Keep Bouncy Castle (used by SSHJ for crypto)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep SLF4J (logging)
-dontwarn org.slf4j.**

# Keep Koin
-keep class org.koin.** { *; }

# AndroidX Security / Tink - Keep annotation classes
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Keep Tink crypto classes (used by AndroidX Security)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep EdDSA crypto classes
-dontwarn sun.security.x509.X509Key
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn net.i2p.crypto.eddsa.**
