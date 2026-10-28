package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** iOS: guarda el archivo en la carpeta temporal y abre la hoja de compartir del sistema. */
@Composable
actual fun rememberFileSharer(): FileSharer = remember { FileSharer { file -> share(file) } }

@OptIn(ExperimentalForeignApi::class)
private fun share(file: SharedFile) {
    val path = NSTemporaryDirectory() + file.name
    file.bytes.toNSData().writeToFile(path, atomically = true)

    val controller = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    // En iPad la hoja sale como popover y necesita de dónde "colgar"
    controller.popoverPresentationController?.sourceView = root.view
    root.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
