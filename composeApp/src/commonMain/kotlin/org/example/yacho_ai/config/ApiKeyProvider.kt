package org.example.yacho_ai.config

interface ApiKeyProvider {
    fun getGoogleApiKey(): String
}

expect fun createApiKeyProvider(): ApiKeyProvider