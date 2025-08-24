package org.example.yacho_ai.config

import java.io.File
import java.util.*

class JvmApiKeyProvider : ApiKeyProvider {
    override fun getGoogleApiKey(): String {
        val localPropertiesFile = File("../local.properties")
        if (localPropertiesFile.exists()) {
            try {
                val properties = Properties()
                properties.load(localPropertiesFile.inputStream())
                val apiKey = properties.getProperty("GOOGLE_API_KEY", "")
                if (apiKey.isNotEmpty()) {
                    return apiKey
                }
            } catch (e: Exception) {
                // Ignore and fallback to environment variable
            }
        }

        // Fallback to environment variable
        return System.getenv("GOOGLE_API_KEY") ?: ""
    }
}

actual fun createApiKeyProvider(): ApiKeyProvider = JvmApiKeyProvider()
