package com.elhafri.foodtracker_ayman

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _profileData = MutableLiveData<UserProfileResponse?>()
    val profileData: LiveData<UserProfileResponse?> = _profileData

    fun loadProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getUserProfile()
                _profileData.postValue(result)
            } catch (e: Exception) {
                _error.postValue("Erreur de chargement: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun saveProfile(weight: Double, height: Double, gender: String, goal: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "Utilisateur non connecté"
            return
        }

        _isLoading.value = true
        val profileRequest = UserProfileRequest(
            uid = userId,
            weight = weight,
            height = height,
            gender = gender,
            goal = goal
        )

        viewModelScope.launch {
            try {
                val result = repository.saveUserProfile(profileRequest)
                if (result != null) {
                    _saveResult.postValue(true)
                } else {
                    _error.postValue("Erreur lors de la sauvegarde sur le serveur")
                }
            } catch (e: Exception) {
                _error.postValue("Erreur réseau: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
