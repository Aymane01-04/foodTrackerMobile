package com.elhafri.foodtracker_ayman

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AddMealViewModel(
    private val repository: MealRepository = MealRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _analysisResult = MutableLiveData<AnalysisResponse?>()
    val analysisResult: LiveData<AnalysisResponse?> = _analysisResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun analyzeMealText(name: String, ingredients: String) {
        val user = auth.currentUser
        if (user == null) {
            _error.value = "Utilisateur non connecté"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val token = user.getIdToken(true).await().token
                val request = MealTextAnalysisRequest(user.uid, name, ingredients)
                val response = repository.analyzeTextMeal(request, "Bearer $token")
                _analysisResult.postValue(response)
            } catch (e: Exception) {
                _error.postValue("Erreur d'analyse : ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun analyzeMealImage(imageFile: File) {
        val user = auth.currentUser
        if (user == null) {
            _error.value = "Utilisateur non connecté"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val token = user.getIdToken(true).await().token
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
                
                val response = repository.analyzeImageMeal(body, "Bearer $token")
                _analysisResult.postValue(response)
            } catch (e: Exception) {
                _error.postValue("Erreur image : ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
