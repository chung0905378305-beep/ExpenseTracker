package com.expensetracker.app.data.remote.api

import com.expensetracker.app.domain.model.AIMessage
import com.expensetracker.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsRepo: SettingsRepository
) {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(messages: List<AIMessage>, systemPrompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepo.get()
            if (settings.aiBaseUrl.isBlank() || settings.aiApiKey.isBlank()) {
                return@withContext "AI 服务未配置，请先设置 API 地址和密钥"
            }

            val baseUrl = settings.aiBaseUrl.trimEnd('/')
            val apiKey = settings.aiApiKey
            val model = settings.aiModel.ifBlank { "gpt-3.5-turbo" }

            val messagesArray = JSONArray()

            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemPrompt)
            messagesArray.put(systemMsg)

            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }

            val requestBody = JSONObject()
            requestBody.put("model", model)
            requestBody.put("messages", messagesArray)

            val url = "$baseUrl/v1/chat/completions"
            val body = requestBody.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = suspendCancellableCoroutine<String?> { cont ->
                val call = okHttpClient.newCall(request)
                cont.invokeOnCancellation { call.cancel() }
                call.enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        cont.resume(null) {}
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        if (response.isSuccessful) {
                            cont.resume(response.body?.string()) {}
                        } else {
                            cont.resume(null) {}
                        }
                    }
                })
            }

            if (response == null) return@withContext "AI 服务连接失败，请检查网络和配置"

            val json = JSONObject(response)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                firstChoice.getJSONObject("message").optString("content", "")
            } else {
                "AI 未返回有效回复"
            }
        } catch (e: Exception) {
            "AI 服务异常: ${e.message ?: "未知错误"}"
        }
    }
}
