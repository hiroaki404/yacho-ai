package org.example.yacho_ai.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import org.example.yacho_ai.config.createApiKeyProvider

class ApiKeyNotConfiguredException(message: String) : IllegalArgumentException(message)

suspend fun runAgent(input: String): String {
    val apiKeyProvider = createApiKeyProvider()
    val apiKey = apiKeyProvider.getGoogleApiKey()
    
    if (apiKey.isEmpty()) {
        throw ApiKeyNotConfiguredException("Google API Key is not configured. Please add GOOGLE_API_KEY to local.properties")
    }

    val executor = simpleGoogleAIExecutor(apiKey)
    val agent = AIAgent(
        executor = executor,
        llmModel = GoogleModels.Gemini2_0Flash
    )

    return agent.run(input)
}
