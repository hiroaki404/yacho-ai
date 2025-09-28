package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.StructuredResponse
import kotlin.reflect.typeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.yacho_ai.config.createApiKeyProvider

class ApiKeyNotConfiguredException(message: String) : IllegalArgumentException(message)

sealed interface TextMessage {
    val content: String
}

sealed interface StructuredMessage {
    val content: SpecifyYachoResult
}

sealed interface ImageInput {
    val image: ByteArray
}

sealed interface ChatMessage {
    data class User(override val content: String) : ChatMessage, TextMessage
    data class UserImage(
        override val image: ByteArray,
        override val content: String
    ) : ChatMessage, ImageInput, TextMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UserImage

            if (!image.contentEquals(other.image)) return false
            if (content != other.content) return false

            return true
        }

        override fun hashCode(): Int {
            var result = image.contentHashCode()
            result = 31 * result + content.hashCode()
            return result
        }
    }

    data class Assistant(override val content: String) : ChatMessage, TextMessage
    data class ToolCall(override val content: String) : ChatMessage, TextMessage
    data class Structured(override val content: SpecifyYachoResult) : ChatMessage, StructuredMessage
}


object ChatAgent {
    private val _chat: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat
    val askToUseInUITool = AskUserInUI
    val apiKeyProvider = createApiKeyProvider()
    val apiKey = apiKeyProvider.getGoogleApiKey()

    fun inputResponse(input: String) {
        _chat.value += ChatMessage.User(input)
        askToUseInUITool.setUserInput(input)
    }

    suspend fun runAgent(input: String, onTakeAssistantMessage: () -> Unit): String {
        _chat.value += ChatMessage.User(input)

        if (apiKey.isEmpty()) {
            throw ApiKeyNotConfiguredException("Google API Key is not configured. Please add GOOGLE_API_KEY to local.properties")
        }

        val toolRegistry = ToolRegistry {
            tool(askToUseInUITool)
        }

        val agent = AIAgent(
            inputType = typeOf<MultiModalInput>(),
            outputType = typeOf<String>(),
            promptExecutor = simpleOpenAIExecutor(apiKey),
            strategy = yachoAgentStrategy(),
            agentConfig = AIAgentConfig(
                prompt = prompt(
                    id = "chat",
                    params = LLMParams(
                        temperature = 1.0,
                        numberOfChoices = 1
                    )
                ) {
                    system("")
                },
                model = OpenAIModels.Chat.GPT4_1,
                maxAgentIterations = 50,
            ),
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onAfterNode {
                    if (it.node.name == "nodeCallLLMWithStructuredResult") {
                        val result = (it.output as Result<*>)
                        if (result.isSuccess) {
                            val structuredResponse = result.getOrThrow() as StructuredResponse<*>
                            val specifyYachoResult = structuredResponse.structure as SpecifyYachoResult
                            _chat.value += ChatMessage.Structured(specifyYachoResult)
                        }
                    }

                    // Debug
                    println("🌲after node  ${it.node.name} : ${it.output}")
                    println("--------------------------------")
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
                        _chat.value += ChatMessage.Assistant((it.toolArgs as AskUserInUI.Args).message)
                        // TODO: display tool call message
                    }
                }
                onAgentFinished {
                    _chat.value += ChatMessage.Assistant("${it.result}")
                    _chat.value += ChatMessage.Assistant("Identified a wild bird. Chat finished")

                    // Debug
                    println("🎥Agent finished")
                }
            }
        }

        return agent.run(MultiModalInput(input))
    }
}
