package org.vicpoo.gasolineproject.core.di

import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.core.session.DATASTORE_FILE_NAME
import org.vicpoo.gasolineproject.core.session.createDataStore

actual val platformModule: Module = module {
    single { AppDirectories(filesDir = androidContext().filesDir.absolutePath) }
    single<FileSystem> { FileSystem.SYSTEM }
    single {
        val directories = get<AppDirectories>()
        createDataStore { "${directories.filesDir}/$DATASTORE_FILE_NAME" }
    }
}
