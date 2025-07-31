package com.example.roadconditions

import android.Manifest
import android.provider.Settings
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var signal: TextView
    private lateinit var trackingInfo: TextView
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        signal = findViewById(R.id.signalStrength)
        trackingInfo = findViewById(R.id.trackingInfo)
        trackingInfo.setText(R.string.Off)
        requestPermissions()

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        addCustomMarker()
        map.getUiSettings().isZoomControlsEnabled = true
        map.getUiSettings().isMapToolbarEnabled = false
    }


    private var foregroundPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val activityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false else true

        if ((fineLocation || coarseLocation) && activityRecognition) {
            // Foreground permissions granted — now ask for background location if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestBackgroundLocationPermission()
            } else {
                startAppFunctions()
            }
        } else {
            Toast.makeText(this, "Foreground location or activity recognition permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private var backgroundPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAppFunctions()
        } else {
            startAppFunctions()
            showBackgroundLocationDialog()
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        foregroundPermissionRequest.launch(permissionsToRequest.toTypedArray())
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun showBackgroundLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Needed")
            .setMessage("To track bumps while the app is closed or minimized, please allow 'All the time' location access.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startAppFunctions() {
        setupActivityRecognition()
        setupLocationTracking()
        enableMyLocation()
    }

    private fun setupActivityRecognition() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val client = ActivityRecognition.getClient(this)
            client.requestActivityUpdates(
                10000L,
                getPendingIntent()
            )

        } else {
            Toast.makeText(this, "Activity recognition permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private val inVehicleReceiver = object : BroadcastReceiver() {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getIntExtra("confidence", 0) ?: 0
            startLocationUpdates()
        }
    }

    private val vehicleExitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopLocationUpdates()
        }
    }

    private fun setupLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location: Location in locationResult.locations) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng))
                    }
                }
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                signal.setText(R.string.strong)

            } else
                signal.setText(R.string.weak)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {

        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
        trackingInfo.setText(R.string.On)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            signal.setText(R.string.strong)

        } else
            signal.setText(R.string.weak)

    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            inVehicleReceiver,
            IntentFilter("activity_in_vehicle_detected")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            vehicleExitReceiver,
            IntentFilter("activity_vehicle_exit_detected")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(inVehicleReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vehicleExitReceiver)
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            signal.setText(R.string.none)
            trackingInfo.setText(R.string.Off)
            sensorManager.unregisterListener(this)
        }
    }

    private var lastBumpTime = 0L
    private val bumpCooldown = 2000L

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val z = it.values[2]

            val verticalAcceleration = Math.abs(z - SensorManager.GRAVITY_EARTH)

            val bumpThreshold = 15.0f

            val currentTime = System.currentTimeMillis()
            if (verticalAcceleration > bumpThreshold && currentTime - lastBumpTime > bumpCooldown) {
                lastBumpTime = currentTime
                onBumpDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onBumpDetected() {
        Toast.makeText(this, "Bump detected", Toast.LENGTH_SHORT).show()
    }

    private fun addCustomMarker() {
        // Mock data
        val locations = listOf(LatLng(61.462087, 23.843748),
            LatLng(61.465286, 23.812305),
            LatLng(61.471189, 23.861733)
        )
        for (location in locations) {
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Possible Bump Detected")
                .snippet("Testi")

            googleMap.addMarker(markerOptions)
        }
        val tampere = LatLng(61.497234,23.759126)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tampere, 12f))
    }
}