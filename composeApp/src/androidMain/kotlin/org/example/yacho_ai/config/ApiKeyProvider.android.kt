package org.example.yacho_ai.config

import org.example.yacho_ai.BuildConfig

class AndroidApiKeyProvider : ApiKeyProvider {
    override fun getGoogleApiKey(): String {
        return BuildConfig.GOOGLE_API_KEY
    }
}

actual fun createApiKeyProvider(): ApiKeyProvider = AndroidApiKeyProvider()