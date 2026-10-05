package com.yimu.ai.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yimu.ai.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiChatClient(
    var apiKey: String,
    var baseUrl: String = "https://api.xiaomimimo.com/v1/chat/completions",
    var model: String = "mimo-v2.6-flash"
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
        messages: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                throw IllegalArgumentException("请先在【设置】中配置你的 AI API Key（如小米 MiMo / DeepSeek / OpenAI）！")
            }

            val requestBodyObj = JsonObject()
            requestBodyObj.addProperty("model", model.trim())
            requestBodyObj.addProperty("temperature", 0.6)

            val apiMessages = JsonArray()

            // 1. 系统账本上下文
            val sysMsg = JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            }
            apiMessages.add(sysMsg)

            // 2. 对话历史与多模态内容 (支持文本 + 图片Base64)
            for (msg in messages) {
                val msgObj = JsonObject().apply {
                    addProperty("role", if (msg.isUser) "user" else "assistant")
                    if (msg.imageBase64.isNullOrBlank()) {
                        addProperty("content", msg.text)
                    } else {
                        // 多模态消息体 (OpenAI / MiMo 规范)
                        val parts = JsonArray()
                        val textPart = JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", if (msg.text.isNotBlank()) msg.text else "请分析这张账单/消费小票图片")
                        }
                        parts.add(textPart)

                        val imgPart = JsonObject().apply {
                            addProperty("type", "image_url")
                            val urlObj = JsonObject().apply {
                                val urlStr = if (msg.imageBase64.startsWith("data:")) {
                                    msg.imageBase64
                                } else {
                                    "data:image/jpeg;base64,${msg.imageBase64}"
                                }
                                addProperty("url", urlStr)
                            }
                            add("image_url", urlObj)
                        }
                        parts.add(imgPart)
                        add("content", parts)
                    }
                }
                apiMessages.add(msgObj)
            }

            requestBodyObj.add("messages", apiMessages)

            // 智能补全 endpoint
            val targetUrl = when {
                baseUrl.endsWith("/chat/completions") -> baseUrl.trim()
                baseUrl.endsWith("/") -> "${baseUrl.trim()}chat/completions"
                else -> "${baseUrl.trim()}/chat/completions"
            }

            val jsonBody = gson.toJson(requestBodyObj)
            val request = Request.Builder()
                .url(targetUrl)
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
