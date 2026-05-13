package com.elhafri.foodtracker_ayman

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NutritionViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {

    private val _nutritionalNeeds = MutableLiveData<NutritionalNeeds>()
    val nutritionalNeeds: LiveData<NutritionalNeeds> = _nutritionalNeeds

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile()
                if (profile != null) {
                    // Si le backend fournit déjà les calculs, on les utilise
                    if (profile.calories != null && profile.proteins != null) {
                        _nutritionalNeeds.postValue(NutritionalNeeds(
                            calories = profile.calories,
                            proteins = profile.proteins,
                            carbs = profile.carbs ?: 0,
                            fat = profile.fat ?: 0
                        ))
                    } else {
                        // Sinon on calcule côté client comme avant
                        calculateNeeds(profile)
                    }
                } else {
                    _error.postValue("Profil non trouvé sur le serveur")
                }
            } catch (e: Exception) {
                _error.postValue("Erreur réseau: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun calculateNeeds(profile: UserProfileResponse) {
        val weight = profile.weight
        val goal = profile.goal

        // Calcul des calories
        var totalCalories = (weight * 30).toInt()
        when (goal) {
            "Perte de poids" -> totalCalories -= 500
            "Prise de masse" -> totalCalories += 500
        }

        // Répartition des macros
        val proteins = (weight * 2).toInt()
        val fat = (weight * 1).toInt()
        
        val proteinCalories = proteins * 4
        val fatCalories = fat * 9
        val carbs = (totalCalories - proteinCalories - fatCalories) / 4

        _nutritionalNeeds.postValue(NutritionalNeeds(
            calories = if (totalCalories < 0) 0 else totalCalories,
            proteins = proteins,
            carbs = if (carbs < 0) 0 else carbs,
            fat = fat
        ))
    }
}
