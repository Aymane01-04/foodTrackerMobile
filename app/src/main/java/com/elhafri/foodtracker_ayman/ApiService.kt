package com.elhafri.foodtracker_ayman

import okhttp3.MultipartBody
import retrofit2.http.*

data class SyncUserRequest(
    val uid: String,
    val email: String
)

data class AnalysisResponse(
    val meal_name: String? = null,
    val plat: String? = null, // Support both names
    val description: String? = null,
    val calories: Int? = null,
    val proteins: Int? = null,
    val proteines: Int? = null, // Support both names
    val carbs: Int? = null,
    val glucides: Int? = null, // Support both names
    val fat: Int? = null,
    val lipides: Int? = null, // Support both names
    val confiance: String? = null,
    val note: String? = null,
    val error: String? = null
) {
    val displayMealName: String? get() = plat ?: meal_name
    val displayProteins: Int get() = proteines ?: proteins ?: 0
    val displayCarbs: Int get() = glucides ?: carbs ?: 0
    val displayFat: Int get() = lipides ?: fat ?: 0
}

data class MealTextAnalysisRequest(
    val uid: String,
    val meal_name: String,
    val ingredients: String
)

interface ApiService {
    @POST("auth/sync-user")
    suspend fun syncUser(@Body request: SyncUserRequest): Any

    @POST("data/profile")
    suspend fun saveProfile(@Body profile: UserProfileRequest): UserProfileResponse

    @GET("data/profile/{uid}")
    suspend fun checkProfileExists(@Path("uid") uid: String): Map<String, Boolean>

    @GET("data/profile-data/{uid}")
    suspend fun getProfileData(@Path("uid") uid: String): UserProfileResponse

    @Multipart
    @POST("data/analyze-meal")
    suspend fun analyzeMeal(
        @Part image: MultipartBody.Part,
        @Header("Authorization") token: String
    ): AnalysisResponse

    @POST("data/analyze-text-meal")
    suspend fun analyzeTextMeal(
        @Body request: MealTextAnalysisRequest,
        @Header("Authorization") token: String
    ): AnalysisResponse
}
