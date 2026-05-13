package com.elhafri.foodtracker_ayman

import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val apiService: ApiService = RetrofitClient.instance
) {
    suspend fun checkProfileExists(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val response = apiService.checkProfileExists(userId)
            response["exists"] ?: false
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking profile", e)
            false
        }
    }

    suspend fun syncUserWithBackend(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val request = SyncUserRequest(uid = user.uid, email = user.email ?: "")
            apiService.syncUser(request)
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error syncing user", e)
            false
        }
    }

    suspend fun getUserProfile(): UserProfileResponse? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            apiService.getProfileData(userId)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting profile data", e)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfileRequest): UserProfileResponse? {
        return try {
            apiService.saveProfile(profile)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving profile", e)
            null
        }
    }
}
