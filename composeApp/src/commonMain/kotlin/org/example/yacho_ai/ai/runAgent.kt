package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun runAgent(input: String): String {
    val apiKey =
        requireNotNull(System.getenv("GOOGLE_API_KEY")) { "Please set the GOOGLE_API_KEY environment variable." }

    val executor = simpleGoogleAIExecutor(apiKey)
    val agent = AIAgent(
        executor = executor,
        llmModel = GoogleModels.Gemini2_0Flash
    )

    return agent.run(input)
}
