package com.zephyrus.di

import com.zephyrus.data.ServerConfigRepository
import com.zephyrus.security.SecureKeyStore
import com.zephyrus.ssh.SshClient
import com.zephyrus.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Security
    single { SecureKeyStore(androidContext()) }
    
    // Data
    single { ServerConfigRepository(androidContext()) }
    
    // SSH
    single { SshClient() }
    
    // ViewModel
    viewModel { MainViewModel(get(), get(), get()) }
}

