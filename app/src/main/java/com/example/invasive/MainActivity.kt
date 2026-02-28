package com.example.invasive

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {

    private lateinit var modelHelper: ModelHelper
    private lateinit var riskAgent: RiskAgent
    private lateinit var actionAgent: ActionAgent
    private lateinit var feedbackAgent: FeedbackAgent
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView

    // 📷 Camera launcher
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { processCapturedImage(it) }
        }

    // 🖼 Gallery launcher
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                processCapturedImage(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        modelHelper = ModelHelper(this)
        riskAgent = RiskAgent()
        actionAgent = ActionAgent()
        feedbackAgent = FeedbackAgent(riskAgent)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultText)
        val captureButton: Button = findViewById(R.id.button)

        captureButton.setOnClickListener {
            showImageSourceDialog()
        }
    }

    // 📌 Show Camera / Gallery chooser
    private fun showImageSourceDialog() {

        val options = arrayOf("Take Photo", "Select from Gallery")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Image Source")

        builder.setItems(options) { _, which ->
            when (which) {

                0 -> { // Camera
                    if (hasPermissions()) {
                        cameraLauncher.launch(null)
                    } else {
                        requestPermissions()
                    }
                }

                1 -> { // Gallery
                    galleryLauncher.launch("image/*")
                }
            }
        }

        builder.show()
    }

    // ✅ Permission check
    private fun hasPermissions(): Boolean {

        val cameraPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val locationPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return cameraPermission && locationPermission
    }

    // ✅ Request permissions
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            101
        )
    }

    // ✅ Permission callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 101) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                cameraLauncher.launch(null)
            }
        }
    }

    // 🔥 Process Image
    private fun processCapturedImage(bitmap: Bitmap) {

        imageView.setImageBitmap(bitmap)

        val (label, confidence) = modelHelper.predict(bitmap)
        val state = PlantState(label, confidence)

        val riskLevel = riskAgent.assessRisk(state)
        val recommendation = actionAgent.suggestAction(riskLevel)

        resultText.text = """
🌿 Species: ${state.species}
📊 Confidence: ${"%.2f".format(state.confidence * 100)}%
⚠ Risk Level: $riskLevel

$recommendation
        """.trimIndent()

        if (riskLevel == "HIGH") {
            savePlantLocation(state.species)
        }
    }

    // 📍 Save REAL-TIME GPS (not cached)
    private fun savePlantLocation(species: String) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {

                val location = locationResult.lastLocation ?: return

                val lat = location.latitude
                val lon = location.longitude
                val time = System.currentTimeMillis()

                saveToLocalStorage(species, lat, lon, time)

                resultText.append(
                    "\n\n📍 Fresh GPS Location:\nLatitude: $lat\nLongitude: $lon"
                )

                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    // 💾 Save locally in SharedPreferences
    private fun saveToLocalStorage(
        species: String,
        lat: Double,
        lon: Double,
        time: Long
    ) {

        val prefs =
            getSharedPreferences("plant_data", Context.MODE_PRIVATE)

        val editor = prefs.edit()
        val entry = "$species,$lat,$lon,$time"

        editor.putString(time.toString(), entry)
        editor.apply()
    }
}