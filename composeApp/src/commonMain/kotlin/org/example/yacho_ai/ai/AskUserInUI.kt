package org.example.yacho_ai.ai

import ai.koog.agents.core.tools.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

object AskUserInUI : SimpleTool<AskUserInUI.Args>() {
    private val _userInput: MutableStateFlow<String> = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput

    @Serializable
    data class Args(val message: String) : ToolArgs

    override val argsSerializer: KSerializer<Args> = Args.serializer()

    override val descriptor: ToolDescriptor = ToolDescriptor(
        name = "__ask_user_in_ui__",
        description = "Service tool, used by the agent to talk with user",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "message", description = "Message from the agent", type = ToolParameterType.String
            )
        )
    )

    fun setUserInput(input: String) {
        _userInput.value = input
    }

    override suspend fun doExecute(args: Args): String {
        println(args.message)
        return userInput.first { it.isNotEmpty() }.also {
            _userInput.value = ""
        }
    }
}
