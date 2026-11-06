package org.vicpoo.gasolineproject.data.di

import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.session.KeychainTokenStore
import org.vicpoo.gasolineproject.domain.session.CachedTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenStore

actual val dataPlatformModule: Module = module {
    single<FileSystem> { FileSystem.SYSTEM }

    // Tokens de la sesión en el Keychain (solo este dispositivo)
    single<TokenStore> { CachedTokenStore(KeychainTokenStore(json = get())) }
}
