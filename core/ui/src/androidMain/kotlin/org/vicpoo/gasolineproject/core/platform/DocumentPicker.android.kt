package org.vicpoo.gasolineproject.core.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberDocumentPicker(
    mimeTypes: List<String>,
    onResult: (PickedFile?) -> Unit,
): DocumentPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            currentOnResult(null)
        } else {
            scope.launch {
                val file = withContext(Dispatchers.IO) { readPickedFile(context, uri) }
                currentOnResult(file)
            }
        }
    }

    return remember(launcher, mimeTypes) {
        DocumentPickerLauncher { launcher.launch(mimeTypes.toTypedArray()) }
    }
}

private fun readPickedFile(context: Context, uri: Uri): PickedFile? = runCatching {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "documento"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    PickedFile(name = name, mimeType = mimeType, bytes = bytes)
}.getOrNull()
