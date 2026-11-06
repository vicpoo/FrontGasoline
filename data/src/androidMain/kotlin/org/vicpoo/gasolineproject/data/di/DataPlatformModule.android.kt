package org.vicpoo.gasolineproject.data.di

import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.session.KeystoreTokenStore
import org.vicpoo.gasolineproject.domain.session.CachedTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenStore

actual val dataPlatformModule: Module = module {
    single<FileSystem> { FileSystem.SYSTEM }

    // Tokens de la sesión cifrados con una llave del Android Keystore
    single<TokenStore> { CachedTokenStore(KeystoreTokenStore(dataStore = get(), json = get())) }
}
