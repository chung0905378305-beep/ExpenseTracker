package com.expensetracker.app.data.remote.api

import com.google.gson.annotations.SerializedName

/**
 * DTO for membership verification request.
 */
data class MembershipVerifyRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("deviceId")
    val deviceId: String
)

/**
 * DTO for membership activation request.
 */
data class MembershipActivateRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("activationCode")
    val activationCode: String,
    @SerializedName("plan")
    val plan: String
)

/**
 * DTO for membership API response.
 */
data class MembershipResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("membership")
    val membership: MembershipInfo?,
    @SerializedName("message")
    val message: String?
)

/**
 * DTO for membership details.
 */
data class MembershipInfo(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("plan")
    val plan: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("startDate")
    val startDate: String?,
    @SerializedName("endDate")
    val endDate: String?,
    @SerializedName("trialUsed")
    val trialUsed: Boolean,
    @SerializedName("features")
    val features: List<String>?
)

/**
 * DTO for membership plan.
 */
data class MembershipPlanDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Double,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("durationDays")
    val durationDays: Int,
    @SerializedName("features")
    val features: List<String>,
    @SerializedName("popular")
    val popular: Boolean
)
