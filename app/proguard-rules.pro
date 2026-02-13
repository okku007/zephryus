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
