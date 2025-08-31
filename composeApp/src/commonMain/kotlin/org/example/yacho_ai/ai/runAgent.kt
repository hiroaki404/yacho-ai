package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.structure.StructuredResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import org.example.yacho_ai.config.createApiKeyProvider

class ApiKeyNotConfiguredException(message: String) : IllegalArgumentException(message)

sealed interface ChatMessage {
    val content: String

    data class User(override val content: String) : ChatMessage
    data class Assistant(override val content: String) : ChatMessage
    data class ToolCall(override val content: String) : ChatMessage
}


object ChatAgent {
    private val _chat: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat
    val askToUseInUITool = AskUserInUI

    fun inputResponse(input: String) {
        _chat.value += ChatMessage.User(input)
        askToUseInUITool.setUserInput(input)
    }

    suspend fun runAgent(input: String, onTakeAssistantMessage: () -> Unit): String {
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
            strategy = yachoAgentStrategy(),
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onAfterNode {
                    // Debug
                    println("🌲after node  ${it.node.name} : ${it.output}")
                    println("--------------------------------")
                    if (it.node.name == "nodeCallLLMWithStructuredResult") {
                        val result = (it.output as Result<*>)
                        if (result.isSuccess) {
                            val structuredResponse = result.getOrThrow() as StructuredResponse<*>
                            val specifyYachoResult = structuredResponse.structure as SpecifyYachoResult
                            val jsonStringResult = Json { prettyPrint = true }.encodeToString(specifyYachoResult)
                            _chat.value += ChatMessage.Assistant(jsonStringResult)
                        }
                    }
                }
                onAfterLLMCall {
                    // Debug
                    println("📞 LLM Call - Prompt Messages:")
                    it.prompt.messages.forEachIndexed { index, message ->
                        println("  [$index] Role: ${message.role}")
                        println("       Content: ${message.content}")
                        val metaString = message.metaInfo.toString()
                        if (metaString.isNotBlank()) {
                            println("       Meta: $metaString")
                        }
                        println("  ---")
                    }
                    println("--------------------------------")
                }
                onToolCall {
                    _chat.value += ChatMessage.ToolCall("Used tool: ${it.tool.name}")
                    val tool = it.tool
                    if (tool is AskUserInUI) {
                        onTakeAssistantMessage()
                    }
                    // TODO: display tool call message
                }
                onAgentFinished {
                    _chat.value += ChatMessage.Assistant("${it.result}")
                    // Debug
                    println("🎥Agent finished")
                }
            }
        }

        return agent.run(input)
    }
}
