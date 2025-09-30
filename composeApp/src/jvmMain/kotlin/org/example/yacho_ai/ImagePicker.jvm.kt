package org.example.yacho_ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    return remember {
        {
            val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD).apply {
                // Filter for image files
                file = "*.png;*.jpg;*.jpeg"
                isMultipleMode = false
            }

            fileDialog.isVisible = true

            val selectedFile = fileDialog.file
            val selectedDirectory = fileDialog.directory

            if (selectedFile != null && selectedDirectory != null) {
                try {
                    val file = File(selectedDirectory, selectedFile)
                    if (file.exists() && file.extension.lowercase() in listOf("png", "jpg", "jpeg")) {
                        val bytes = file.readBytes()
                        onImagePicked(bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}