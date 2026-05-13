package com.elhafri.foodtracker_ayman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.elhafri.foodtracker_ayman.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private val viewModel: NutritionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Mise à jour automatique du dashboard au retour de l'ajout d'un repas
        viewModel.loadUserData()
    }

    private fun setupUI() {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val currentDate = sdf.format(Date()).uppercase()
        binding.dateTextView.text = currentDate

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.addMealFab.setOnClickListener {
            val intent = Intent(this, AddMealActivity::class.java)
            startActivity(intent)
        }
        
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.nutritionalNeeds.observe(this) { needs ->
            binding.caloriesTextView.text = String.format("%,d", needs.calories)
            binding.proteinTextView.text = "0/${needs.proteins}g"
            binding.carbsTextView.text = "0/${needs.carbs}g"
            binding.fatTextView.text = "0/${needs.fat}g"
            
            // On peut imaginer un calcul réel ici basé sur les repas consommés
            binding.calorieProgressBar.progress = 0
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
