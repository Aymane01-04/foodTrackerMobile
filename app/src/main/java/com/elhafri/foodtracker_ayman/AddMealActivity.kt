package com.elhafri.foodtracker_ayman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.elhafri.foodtracker_ayman.databinding.ActivityAddMealBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddMealActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMealBinding
    private val viewModel: AddMealViewModel by viewModels()
    private var imageUri: Uri? = null
    private var currentTab = "text" // "text" or "image"
    
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            imageUri?.let { handleSelectedImage(it) }
        }
    }

    private val mapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            selectedLat = data?.getDoubleExtra("lat", 0.0)
            selectedLng = data?.getDoubleExtra("lng", 0.0)
            
            if (selectedLat != null && selectedLng != null) {
                binding.locationText.text = "📍 Position fixée"
                binding.locationText.setTextColor(ContextCompat.getColor(this, R.color.accent2))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupTabs()
        setupListeners()
        observeViewModel()
    }

    private fun setupTabs() {
        binding.tabText.setOnClickListener { switchTab("text") }
        binding.tabImage.setOnClickListener { switchTab("image") }
        switchTab("text")
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        if (tab == "text") {
            binding.tabText.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
            binding.tabText.setTextColor(ContextCompat.getColor(this, R.color.black))
            binding.tabImage.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            binding.tabImage.setTextColor(ContextCompat.getColor(this, R.color.muted))
            binding.textPanel.isVisible = true
            binding.imagePanel.isVisible = false
        } else {
            binding.tabImage.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
            binding.tabImage.setTextColor(ContextCompat.getColor(this, R.color.black))
            binding.tabText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            binding.tabText.setTextColor(ContextCompat.getColor(this, R.color.muted))
            binding.textPanel.isVisible = false
            binding.imagePanel.isVisible = true
        }
    }

    private fun setupListeners() {
        binding.dropZone.setOnClickListener { showImageSourceDialog() }
        binding.removeImageBtn.setOnClickListener { removeImage() }
        
        binding.locationRow.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            mapLauncher.launch(intent)
        }
        
        binding.analyzeBtn.setOnClickListener {
            analyze()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("📷 Prendre une photo", "🖼 Choisir depuis la galerie")
        MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter une photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun handleSelectedImage(uri: Uri) {
        imageUri = uri
        binding.mealImageView.setImageURI(uri)
        binding.mealImageView.isVisible = true
        binding.dropZone.isVisible = false
        binding.removeImageBtn.isVisible = true
        binding.resultsLayout.isVisible = false
    }

    private fun removeImage() {
        imageUri = null
        binding.mealImageView.setImageURI(null)
        binding.mealImageView.isVisible = false
        binding.dropZone.isVisible = true
        binding.removeImageBtn.isVisible = false
    }

    private fun analyze() {
        binding.resultsLayout.isVisible = false
        val portion = binding.portionInput.text.toString().toDoubleOrNull() ?: 1.0

        if (currentTab == "text") {
            val text = binding.textInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Décris d'abord ton plat.", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.analyzeMealText(text, "Portion: $portion")
        } else {
            if (imageUri == null) {
                Toast.makeText(this, "Ajoute d'abord une image.", Toast.LENGTH_SHORT).show()
                return
            }
            val file = getFileFromUri(imageUri!!)
            viewModel.analyzeMealImage(file)
        }
    }

    private fun observeViewModel() {
        viewModel.analysisResult.observe(this) { response ->
            if (response != null) {
                if (response.error != null) {
                    Toast.makeText(this, "Erreur: ${response.error}", Toast.LENGTH_LONG).show()
                } else {
                    displayResults(response)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingSpinner.isVisible = isLoading
            binding.analyzeBtn.isEnabled = !isLoading
            binding.analyzeBtn.text = if (isLoading) "Analyse en cours..." else "Analyser les macros →"
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun displayResults(response: AnalysisResponse) {
        binding.resultsLayout.isVisible = true
        binding.resPlatName.text = response.displayMealName ?: "Plat analysé"
        binding.resPlatDesc.text = response.description ?: response.note ?: ""
        
        binding.valCal.text = "${response.calories ?: 0}"
        binding.valProt.text = "${response.displayProteins}g"
        binding.valCarb.text = "${response.displayCarbs}g"
        binding.valFat.text = "${response.displayFat}g"

        val total = (response.displayProteins + response.displayCarbs + response.displayFat).coerceAtLeast(1)
        binding.barProt.progress = (response.displayProteins * 100 / total)
        binding.barCarb.progress = (response.displayCarbs * 100 / total)
        binding.barFat.progress = (response.displayFat * 100 / total)

        val conf = (response.confiance ?: "moyenne").lowercase()
        binding.confText.text = "Confiance : $conf"
        val dotColor = when(conf) {
            "haute" -> R.color.accent2
            "faible" -> R.color.fat
            else -> R.color.carbs
        }
        binding.confDot.setBackgroundColor(ContextCompat.getColor(this, dotColor))
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("MEAL_${timeStamp}_", ".jpg", storageDir)
        imageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
        cameraLauncher.launch(imageUri!!)
    }

    private fun getFileFromUri(uri: Uri): File {
        val file = File(cacheDir, "temp_image.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file
    }
}
