package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.yacho_ai.config.createApiKeyProvider

class ApiKeyNotConfiguredException(message: String) : IllegalArgumentException(message)

sealed interface ChatMessage {
    val content: String

    data class User(override val content: String) : ChatMessage
    data class Assistant(override val content: String) : ChatMessage
}


object ChatAgent {
    private val _chat: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat
    val askToUseInUITool = AskUserInUI

    fun inputResponse(input: String) {
        _chat.value += ChatMessage.User(input)
        askToUseInUITool.setUserInput(input)
    }

    suspend fun runAgent(input: String): String {
        _chat.value += ChatMessage.User(input)

        val apiKeyProvider = createApiKeyProvider()
        val apiKey = apiKeyProvider.getGoogleApiKey()

        if (apiKey.isEmpty()) {
            throw ApiKeyNotConfiguredException("Google API Key is not configured. Please add GOOGLE_API_KEY to local.properties")
        }

        val toolRegistry = ToolRegistry {
            tool(askToUseInUITool)
        }

        val executor = simpleOpenAIExecutor(apiKey)
        val agent = AIAgent(
            executor = executor,
            llmModel = OpenAIModels.Reasoning.GPT4oMini,
            strategy = chatAgentStrategy(),
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onAfterNode {
                    // Debug
                    println("after node " + it.node.name + it.output)
                }
                onToolCall {
                    val tool = it.tool
                    if (tool is AskUserInUI) {
                        val message = (it.toolArgs as AskUserInUI.Args).message
                        _chat.value += ChatMessage.Assistant(message)
                    }
                }
                onAgentFinished {
                    // Debug
                    println("Agent finished")
                }
            }
        }

        return agent.run(input)
    }
}
