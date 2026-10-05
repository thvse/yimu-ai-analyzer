package com.yimu.ai.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiChatClient(
    var apiKey: String,
    var baseUrl: String = "https://api.deepseek.com/chat/completions",
    var model: String = "deepseek-chat"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        systemPrompt: String,
        messages: List<Pair<Boolean, String>> // isUser to text
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                throw IllegalArgumentException("请先在设置中配置你的 AI API Key（如 DeepSeek 或 OpenAI）！")
            }

            val apiMessages = mutableListOf<ApiMessage>()
            // 注入账本上下文系统提示词
            apiMessages.add(ApiMessage(role = "system", content = systemPrompt))

            // 历史对话
            for ((isUser, text) in messages) {
                apiMessages.add(
                    ApiMessage(
                        role = if (isUser) "user" else "assistant",
                        content = text
                    )
                )
            }

            val requestBodyObj = ChatRequest(
                model = model,
                messages = apiMessages,
                temperature = 0.6
            )

            val jsonBody = gson.toJson(requestBodyObj)
            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw IllegalStateException("API 请求失败 [HTTP ${response.code}]: $responseBody")
            }

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("AI 返回了空内容")

            content.trim()
        }
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<ApiMessage>,
        val temperature: Double
    )

    private data class ApiMessage(
        val role: String,
        val content: String
    )

    private data class ChatResponse(
        val choices: List<Choice>
    )

    private data class Choice(
        val message: ChoiceMessage
    )

    private data class ChoiceMessage(
        val role: String,
        val content: String
    )
}
