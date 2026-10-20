// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** No image picker on this platform: the lab falls back to its solid background. */
@Composable
actual fun rememberLabImagePicker(onPicked: (ImageBitmap?) -> Unit): () -> Unit = {}
