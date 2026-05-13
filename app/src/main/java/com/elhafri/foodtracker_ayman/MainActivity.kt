package com.elhafri.foodtracker_ayman

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.elhafri.foodtracker_ayman.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Forcer le mode sombre visuellement pour le splash
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        observeViewModel()
        viewModel.checkUserStatus()
    }

    private fun observeViewModel() {
        viewModel.navigationState.observe(this) { state ->
            when (state) {
                is NavigationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is NavigationState.NavigateToLogin -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is NavigationState.NavigateToProfile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                }
                is NavigationState.NavigateToDashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
            }
        }
    }
}
