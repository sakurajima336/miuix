// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
@Composable
actual fun rememberLabImagePicker(onPicked: (ImageBitmap?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val current = rememberUpdatedState(onPicked)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val bitmap =
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            current.value(bitmap?.asImageBitmap())
        }
    return { launcher.launch("image/*") }
}
