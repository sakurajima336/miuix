// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0

/** No image picker on this platform: the lab falls back to its solid background. */
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
@Composable
actual fun rememberLabImagePicker(onPicked: (ImageBitmap?) -> Unit): () -> Unit = {}
