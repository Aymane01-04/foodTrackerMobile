package com.elhafri.foodtracker_ayman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.elhafri.foodtracker_ayman.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupObservers()
        
        viewModel.loadProfile()

        binding.continueButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.continueButton.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.profileData.observe(this) { profile ->
            if (profile != null) {
                binding.weightEditText.setText(profile.weight.toString())
                binding.heightEditText.setText(profile.height.toString())

                if (profile.gender == "Femme") {
                    binding.femaleRadioButton.isChecked = true
                } else {
                    binding.maleRadioButton.isChecked = true
                }

                when (profile.goal) {
                    "Perte de poids" -> binding.goalToggleGroup.check(R.id.loseWeightButton)
                    "Maintien" -> binding.goalToggleGroup.check(R.id.maintainButton)
                    "Prise de masse" -> binding.goalToggleGroup.check(R.id.gainWeightButton)
                }
            }
        }

        viewModel.saveResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun saveProfile() {
        val weightStr = binding.weightEditText.text.toString()
        val heightStr = binding.heightEditText.text.toString()

        if (weightStr.isEmpty()) {
            binding.weightLayout.error = getString(R.string.error_weight)
            return
        }
        binding.weightLayout.error = null

        if (heightStr.isEmpty()) {
            binding.heightLayout.error = getString(R.string.error_height)
            return
        }
        binding.heightLayout.error = null

        val weight = weightStr.toDoubleOrNull() ?: 0.0
        val height = heightStr.toDoubleOrNull() ?: 0.0

        if (weight <= 0) {
            binding.weightLayout.error = getString(R.string.error_weight)
            return
        }
        if (height <= 0) {
            binding.heightLayout.error = getString(R.string.error_height)
            return
        }

        val gender = if (binding.femaleRadioButton.isChecked) "Femme" else "Homme"
        
        val goal = when (binding.goalToggleGroup.checkedButtonId) {
            R.id.loseWeightButton -> "Perte de poids"
            R.id.maintainButton -> "Maintien"
            R.id.gainWeightButton -> "Prise de masse"
            else -> "Maintien"
        }

        viewModel.saveProfile(weight, height, gender, goal)
    }
}
