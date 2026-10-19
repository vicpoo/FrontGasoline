package org.vicpoo.gasolineproject.core.di

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.core.session.DATASTORE_FILE_NAME
import org.vicpoo.gasolineproject.core.session.createDataStore
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single { AppDirectories(filesDir = iosDocumentsPath()) }
    single<FileSystem> { FileSystem.SYSTEM }
    single {
        val directories = get<AppDirectories>()
        createDataStore { "${directories.filesDir}/$DATASTORE_FILE_NAME" }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosDocumentsPath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
