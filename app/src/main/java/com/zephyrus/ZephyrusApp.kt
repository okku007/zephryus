package com.zephyrus

import android.app.Application
import com.zephyrus.di.appModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.security.Security

class ZephyrusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register Bouncy Castle provider for SSHJ crypto (X25519, etc.)
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        
        startKoin {
            androidLogger()
            androidContext(this@ZephyrusApp)
            modules(appModule)
        }
    }
}
