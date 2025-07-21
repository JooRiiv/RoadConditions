package com.example.roadconditions

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var toggle: ToggleButton

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
        toggle = findViewById(R.id.trackingButton)
        requestPermissions()

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        addCustomMarker()
        map.getUiSettings().isZoomControlsEnabled = true
    }


    private var locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    // Background location access granted.

                }
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||

                        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Approximate or fine location access granted.
                    setupLocationTracking()
                    enableMyLocation()

                }

                else -> {
                    // No location access granted.
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Check if access has already been granted previously.
    private fun requestPermissions () {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    private fun setupLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) startLocationUpdates() else stopLocationUpdates()
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location: Location in locationResult.locations) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng))
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
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
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