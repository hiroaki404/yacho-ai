package org.example.yacho_ai

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.yacho_ai.ai.ChatAgent

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "yacho_ai",
    ) {
        App(ChatAgent)
    }
}
