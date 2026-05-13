package com.elhafri.foodtracker_ayman

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class NavigationState {
    object Loading : NavigationState()
    object NavigateToLogin : NavigationState()
    object NavigateToProfile : NavigationState()
    object NavigateToDashboard : NavigationState()
}

class MainViewModel(
    private val repository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> = _navigationState

    fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _navigationState.value = NavigationState.NavigateToLogin
            return
        }

        _navigationState.value = NavigationState.Loading
        viewModelScope.launch {
            try {
                // 1. Synchroniser l'utilisateur avec le backend
                repository.syncUserWithBackend()
                
                // 2. Vérifier si le profil existe
                val profileExists = repository.checkProfileExists()
                
                if (!profileExists) {
                    _navigationState.postValue(NavigationState.NavigateToProfile)
                } else {
                    _navigationState.postValue(NavigationState.NavigateToDashboard)
                }
            } catch (e: Exception) {
                // En cas d'erreur réseau majeure, on redirige vers Login par sécurité
                _navigationState.postValue(NavigationState.NavigateToLogin)
            }
        }
    }
}
