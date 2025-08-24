package org.example.yacho_ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.yacho_ai.ai.ApiKeyNotConfiguredException
import org.example.yacho_ai.ai.ChatAgent
import org.example.yacho_ai.ai.ChatMessage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(chatAgent: ChatAgent) {
    val chat by chatAgent.chat.collectAsState()

    MaterialTheme {
        var userInput by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        // 新しいメッセージが追加されたときに自動スクロール
        LaunchedEffect(chat.size) {
            if (chat.isNotEmpty()) {
                listState.animateScrollToItem(chat.size - 1)
            }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            // ヘッダー
            Text(
                text = "Yacho AI Chat",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )

            // チャット履歴
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chat) { message ->
                    MessageBubble(message = message)
                }

                // ローディング中のインジケーター
                if (isLoading) {
                    item {
                        LoadingMessageBubble()
                    }
                }
            }

            // 入力フィールド
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("メッセージを入力") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            val message = userInput
                            userInput = ""
                            isLoading = true
                            coroutineScope.launch {
                                if (chat.isEmpty()) {
                                    try {
                                        chatAgent.runAgent(message)
                                    } catch (e: ApiKeyNotConfiguredException) {
                                        // エラー処理は後で実装
                                    } finally {
                                        isLoading = false
                                    }
                                } else {
                                    chatAgent.inputResponse(message)
                                }
                            }
                        }
                    },
                    enabled = !isLoading && userInput.isNotBlank(),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("送信")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message is ChatMessage.User

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LoadingMessageBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "AIが返答を生成中...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
