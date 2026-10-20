// Copyright 2026, COUI contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Opens the platform image picker for the component lab's background.
 *
 * Returns a lambda the caller can invoke; `onPicked` receives the decoded bitmap (or `null` when the
 * user cancels). Platforms without a picker simply return a no-op lambda.
 */
@Composable
expect fun rememberLabImagePicker(onPicked: (ImageBitmap?) -> Unit): () -> Unit
