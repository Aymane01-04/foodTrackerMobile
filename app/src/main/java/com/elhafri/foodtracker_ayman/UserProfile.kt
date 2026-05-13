package com.elhafri.foodtracker_ayman

import com.google.gson.annotations.SerializedName

data class UserProfileRequest(
    val uid: String,
    @SerializedName("poids") val weight: Double,
    @SerializedName("taille") val height: Double,
    @SerializedName("genre") val gender: String,
    @SerializedName("objectif") val goal: String
)

data class UserProfileResponse(
    val uid: String,
    @SerializedName("poids") val weight: Double,
    @SerializedName("taille") val height: Double,
    @SerializedName("genre") val gender: String,
    @SerializedName("objectif") val goal: String,
    val calories: Int? = null,
    val proteins: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null
)
