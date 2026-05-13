package com.elhafri.foodtracker_ayman

import okhttp3.MultipartBody

class MealRepository(private val apiService: ApiService = RetrofitClient.instance) {
    suspend fun analyzeTextMeal(request: MealTextAnalysisRequest, token: String) = 
        apiService.analyzeTextMeal(request, token)

    suspend fun analyzeImageMeal(image: MultipartBody.Part, token: String) = 
        apiService.analyzeMeal(image, token)
}
