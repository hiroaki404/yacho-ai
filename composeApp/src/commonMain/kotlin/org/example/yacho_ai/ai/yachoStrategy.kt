package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.result
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructuredResponse
import ai.koog.prompt.structure.json.JsonSchemaGenerator
import ai.koog.prompt.structure.json.JsonStructuredData
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Structured data representing the result of bird species identification")
data class SpecifyYachoResult(
    @LLMDescription("Confidence score indicating the reliability of the bird identification (0-100 scale, where 100 is most certain)")
    val reliabilityScore: Int,
    @LLMDescription("The identified bird species name (scientific name or common name)")
    val birdName: String,
    @LLMDescription("Detailed description including bird characteristics, habitat, identification points, and other relevant information")
    val description: String,
)

fun yachoAgentStrategy(): AIAgentStrategy<String, String> = strategy("yacho-chat") {
    val userMessages = mutableListOf<String>()
    var toolCallCount = 0

    val nodeUpdatePrompt by node<String, String>("nodeUpdatePrompt") { message ->
        llm.writeSession {
            updatePrompt {
                system(
                    "You are an expert in identifying wild birds. You are responsible for identifying wild birds from user input while using a tool to increase the reliabilityScore to 95. Lower the confidence when there are other potential wild bird candidates and it's difficult to identify a specific one. Supports wild birds in Japan."
                )
            }
        }
        message
    }

    val evaluateBird by subgraph<String, Message.Response> {
        val buildListingBirdPrompt by node<String, String> { userMessage ->
            userMessages.add(userMessage)
            "${userMessages.joinToString("\n")}\n---\nBased on the above input and conversation history, please list about 3 possible wild bird candidates"
            // TODO: Context is forgotten, so conversation history gets lost. Should keep it as memory
        }

        val listingBirdCandidates by nodeLLMRequest(allowToolCalls = false)

        val buildStructedBirdPrompt by node<Message.Response, String> { listingResponse ->
            """
                "${listingResponse.content}"
                Based on the conversation history, please select one top candidate and provide an explanation about it.
            """.trimIndent()
        }

        val nodeCallLLMWithStructuredResult by nodeLLMRequestStructured<SpecifyYachoResult>(
            structure = JsonStructuredData.createJsonStructure<SpecifyYachoResult>(
                schemaFormat = JsonSchemaGenerator.SchemaFormat.JsonSchema,
                examples = listOf(
                    SpecifyYachoResult(
                        reliabilityScore = 35,
                        birdName = "スズメ (Passer montanus)",
                        description = "全長約14cm。頭部は茶褐色で黒い過眼線があり、頬に黒い斑点が特徴的。背中は茶褐色で黒い縦斑、腹部は灰白色。都市部から農村部まで幅広く生息し、群れで行動することが多い。"
                    ),
                    SpecifyYachoResult(
                        reliabilityScore = 48,
                        birdName = "ヒヨドリ (Hypsipetes amaurotis)",
                        description = "全長約28cm。全体的に灰褐色で、頭部がやや黒っぽく、頬から耳羽にかけて褐色。尾は長めで先端が白い。鳴き声が特徴的で「ヒーヨ、ヒーヨ」と聞こえる。公園や住宅地でよく見られる。"
                    ),
                    SpecifyYachoResult(
                        reliabilityScore = 72,
                        birdName = "メジロ (Zosterops japonicus)",
                        description = "全長約12cm。背中は黄緑色、腹部は白色で脇腹に黄色味がある。目の周りの白いアイリングが最大の特徴。花の蜜や果実を好み、梅や桜の花によく訪れる。小さな群れで行動することが多い。"
                    )
                )
            ),
            retries = 2,
            fixingModel = OpenAIModels.CostOptimized.GPT4oMini
        )

        val specifyBird by node<Result<StructuredResponse<SpecifyYachoResult>>, String> {
            "Please identify the wild bird based on the conversation history"
        }

        val callLLM by nodeLLMRequest(allowToolCalls = false)

        nodeStart then buildListingBirdPrompt then listingBirdCandidates then buildStructedBirdPrompt then nodeCallLLMWithStructuredResult then specifyBird then callLLM then nodeFinish
    }

    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by node<ReceivedToolResult, String>("nodeSendToolResult") { result ->
        toolCallCount++
        llm.writeSession {
            updatePrompt {
                system(
                    "You are an expert in identifying wild birds. Please identify the species of wild bird from the user input. You are responsible for identifying wild birds from user input while using a tool to increase the reliabilityScore to 95. Lower the confidence when there are other potential wild bird candidates and it's difficult to identify a specific one."
                )
                tool {
                    result(result)
                }
            }

            requestLLM()
            result.content
        }
    }

    val giveFeedbackToCallTools by node<String, Message.Response> { _ ->
        llm.writeSession {
            updatePrompt {
                user("Don't chat with plain text! Call one of the available tools, instead: ${tools.joinToString(", ") { it.name }}.")
            }

            requestLLM()
        }
    }

    edge(nodeStart forwardTo nodeUpdatePrompt)

    edge(nodeUpdatePrompt forwardTo evaluateBird)

    edge(evaluateBird forwardTo nodeExecuteTool onToolCall { true })
    edge(evaluateBird forwardTo giveFeedbackToCallTools onCondition { toolCallCount == 0 } onAssistantMessage { true })
    edge(evaluateBird forwardTo nodeFinish onCondition { toolCallCount > 0 } onAssistantMessage { true })
    edge(evaluateBird forwardTo nodeFinish onToolCall { tc -> tc.tool == "__exit__" } transformed { "Chat finished" })

    edge(giveFeedbackToCallTools forwardTo giveFeedbackToCallTools onAssistantMessage { true })
    edge(giveFeedbackToCallTools forwardTo nodeExecuteTool onToolCall { true })

    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    edge(nodeSendToolResult forwardTo evaluateBird)
}
