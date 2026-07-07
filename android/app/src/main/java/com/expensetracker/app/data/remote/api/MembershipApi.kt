package com.expensetracker.app.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class MembershipVerifyDto(
    val memberId: String,
    val isActive: Boolean,
    val expiresAt: Long?,
    val plan: String?
)

data class MembershipActivateDto(
    val success: Boolean,
    val memberId: String?,
    val message: String?
)

@Singleton
class MembershipApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val BASE_URL = "http://10.0.2.2:4000"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun verifyMembership(memberId: String): MembershipVerifyDto? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/membership/verify?memberId=${memberId}"
            val request = Request.Builder().url(url).get().build()

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

            if (response == null) return@withContext MembershipVerifyDto(
                memberId = memberId, isActive = false, expiresAt = null, plan = null
            )

            val json = JSONObject(response)
            MembershipVerifyDto(
                memberId = json.optString("memberId", memberId),
                isActive = json.optBoolean("isActive", false),
                expiresAt = json.optLong("expiresAt", 0),
                plan = json.optString("plan", null)
            )
        } catch (_: Exception) {
            MembershipVerifyDto(memberId = memberId, isActive = false, expiresAt = null, plan = null)
        }
    }

    suspend fun activateCode(code: String): MembershipActivateDto? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/membership/activate"
            val body = JSONObject().put("code", code).toString()
                .toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

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

            if (response == null) return@withContext MembershipActivateDto(
                success = false, memberId = null, message = "网络请求失败"
            )

            val json = JSONObject(response)
            MembershipActivateDto(
                success = json.optBoolean("success", false),
                memberId = json.optString("memberId", null),
                message = json.optString("message", null)
            )
        } catch (_: Exception) {
            MembershipActivateDto(success = false, memberId = null, message = "网络请求失败")
        }
    }
}
