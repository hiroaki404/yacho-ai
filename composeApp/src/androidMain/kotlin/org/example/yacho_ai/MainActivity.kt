package org.example.yacho_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.yacho_ai.ai.ChatAgent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val agent = ChatAgent

        setContent {
            App(ChatAgent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(ChatAgent)
}
