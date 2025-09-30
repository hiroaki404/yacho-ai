package org.example.yacho_ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.example.yacho_ai.ai.ApiKeyNotConfiguredException
import org.example.yacho_ai.ai.ChatAgent
import org.example.yacho_ai.ai.ChatMessage
import org.example.yacho_ai.ui.LoadingMessageBubble
import org.example.yacho_ai.ui.MessageBubble
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(chatAgent: ChatAgent, modifier: Modifier = Modifier) {
    val chat by chatAgent.chat.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages are added
    LaunchedEffect(chat.size) {
        if (chat.isNotEmpty()) {
            listState.animateScrollToItem(chat.size - 1)
        }
    }

    MaterialTheme {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            AppHeader()

            ChatHistoryList(
                messages = chat,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            ChatInputSection(
                chatAgent = chatAgent,
                isLoading = isLoading,
                onLoadingChange = { isLoading = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun AppHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text(
            text = "Yacho AI",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Japanese bird identification app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Please enter information about the bird you saw",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}


@Composable
fun ChatHistoryList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }

        if (isLoading) {
            item {
                LoadingMessageBubble()
            }
        }
    }
}

@Composable
fun ImagePreviewCard(
    imageBytes: ByteArray,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = imageBytes,
                contentDescription = "Selected image preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputSection(
    chatAgent: ChatAgent,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val chat by chatAgent.chat.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Image preview
        selectedImage?.let { imageBytes ->
            ImagePreviewCard(
                imageBytes = imageBytes,
                onRemove = { selectedImage = null },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image selection button
            val imagePickerLauncher = rememberImagePickerLauncher { imageBytes ->
                selectedImage = imageBytes
            }

            IconButton(
                onClick = { imagePickerLauncher() },
                enabled = !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Enter message") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )

            Button(
                onClick = {
                    if (userInput.isNotBlank() || selectedImage != null) {
                        val message = userInput
                        val image = selectedImage
                        userInput = ""
                        selectedImage = null
                        onLoadingChange(true)
                        coroutineScope.launch {
                            if (chat.isEmpty()) {
                                try {
                                    if (image != null) {
                                        // TODO: Send image message
                                        // chatAgent.sendImageMessage(message, image)
                                    } else {
                                        chatAgent.runAgent(message) {
                                            onLoadingChange(false)
                                        }
                                    }
                                } catch (e: ApiKeyNotConfiguredException) {
                                    // Error handling will be implemented later
                                } finally {
                                    onLoadingChange(false)
                                }
                            } else {
                                if (image != null) {
                                    // TODO: Send image message
                                    // chatAgent.sendImageMessage(message, image)
                                } else {
                                    chatAgent.inputResponse(message)
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading && (userInput.isNotBlank() || selectedImage != null),
                modifier = Modifier.height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        App(chatAgent = ChatAgent, modifier = Modifier)
    }
}
