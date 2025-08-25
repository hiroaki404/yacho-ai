package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.result
import ai.koog.prompt.message.Message

fun yachoAgentStrategy(): AIAgentStrategy<String, String> = strategy("yacho-chat") {

    val nodeCallLLM by node<String, Message.Response>("sendInput") { message ->
        llm.writeSession {
            updatePrompt {
                system(
                    "You are an expert in identifying wild birds. Please identify the species of wild bird from the user input."
                )
                user(message)
            }

            requestLLMWithoutTools()
        }
    }
    val nodeExecuteTool by nodeExecuteTool("nodeExecuteTool")
    val nodeSendToolResult by node<ReceivedToolResult, Message.Response>("nodeSendToolResult") { result ->
        llm.writeSession {
            updatePrompt {
                system(
                    "You are an expert in identifying wild birds. Please identify the species of wild bird from the user input."
                )
                tool {
                    result(result)
                }
            }

            requestLLM()
        }
    }

    val giveFeedbackToCallTools by node<String, Message.Response> { _ ->
        llm.writeSession {
            updatePrompt {
                user("Don't chat with plain text! Call one of the available tools, instead: ${tools.joinToString(", ") { it.name }}")
            }

            requestLLM()
        }
    }

    edge(nodeStart forwardTo nodeCallLLM)

    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeCallLLM forwardTo giveFeedbackToCallTools onAssistantMessage { true })

    edge(giveFeedbackToCallTools forwardTo giveFeedbackToCallTools onAssistantMessage { true })
    edge(giveFeedbackToCallTools forwardTo nodeExecuteTool onToolCall { true })

    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeSendToolResult forwardTo nodeFinish onToolCall { tc -> tc.tool == "__exit__" } transformed { "Chat finished" })
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}
