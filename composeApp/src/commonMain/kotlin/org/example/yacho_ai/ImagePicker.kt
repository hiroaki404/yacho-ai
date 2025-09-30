package org.example.yacho_ai

import androidx.compose.runtime.Composable

/**
 * Platform-specific image picker launcher
 * Returns a lambda that launches the image picker when invoked
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): () -> Unit