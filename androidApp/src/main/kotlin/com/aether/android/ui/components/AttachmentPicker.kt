package com.aether.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Opens the system file picker (Storage Access Framework) restricted to
 * [mimeTypes] and persists read permission on whatever the user picks, so
 * the URI stays valid across app restarts without ever copying the file
 * into app-private storage. Returns a zero-arg function — call it to launch
 * the picker.
 */
@Composable
fun rememberAttachmentPicker(mimeTypes: Array<String>, onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onPicked(uri)
    }
    return { launcher.launch(mimeTypes) }
}

/** Opens whatever [uriString] points to in an external viewer app. */
fun openAttachment(context: android.content.Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
