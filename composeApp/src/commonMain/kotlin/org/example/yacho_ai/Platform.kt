package org.example.yacho_ai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform