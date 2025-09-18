package com.example.roadconditions

import BumpClusterItem
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var signal: TextView
    private lateinit var trackingInfo: TextView
    private lateinit var clusterManager: ClusterManager<BumpClusterItem>
    private lateinit var infoWindowAdapter: CustomInfoWindowAdapter

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
        signal = findViewById(R.id.signalStrength)
        trackingInfo = findViewById(R.id.trackingInfo)
        trackingInfo.setText(R.string.Off)
        requestPermissions()
    }

    @SuppressLint("PotentialBehaviorOverride")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        clusterManager = ClusterManager(this, googleMap)

        infoWindowAdapter = CustomInfoWindowAdapter(this)
        clusterManager.markerCollection.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)

        map.getUiSettings().isZoomControlsEnabled = true
        map.getUiSettings().isMapToolbarEnabled = false

        showBumpsOnMap()
    }

    private var foregroundPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        }
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val activityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false else true

        if ((fineLocation || coarseLocation) && activityRecognition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBackgroundLocationPermission()
            } else {
                startAppFunctions()
            }
        } else {
            Toast.makeText(
                this,
                "Foreground location or activity recognition permission denied",
                Toast.LENGTH_SHORT
            ).show()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
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
                3000L,
                getPendingIntent()
            )

        } else {
            Toast.makeText(this, "Activity recognition permission not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private val vehicleStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val inVehicle = intent?.getBooleanExtra("inVehicle", false) ?: false
            trackingInfo.setText(if (inVehicle) R.string.On else R.string.Off)
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            vehicleStateReceiver,
            IntentFilter("vehicle_state_changed")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vehicleStateReceiver)
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
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
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
        }
    }
    object BumpClient {
        private const val ENDPOINT_URL =
            "https://roadconditions.yellowglacier-a8220dfb.norwayeast.azurecontainerapps.io/bumps"
        private val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getAllBumps(): List<Bump> {
            return client.get(ENDPOINT_URL)
                .body()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBumpsOnMap() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                val bumps = BumpClient.getAllBumps()

                withContext(Dispatchers.Main) {
                    clusterManager.clearItems()
                    for (bump in bumps) {
                        val parsedDate = inputFormat.parse(bump.timestamp)
                        val formattedDate = parsedDate?.let { outputFormat.format(it) } ?: bump.timestamp

                        val item = BumpClusterItem(
                            bump.latitude,
                            bump.longitude,
                            "A possible bump detected",
                            "Date and time: $formattedDate\nAccuracy: ${bump.signal}"
                        )
                        clusterManager.addItem(item)
                    }
                    clusterManager.cluster()
                }
            } catch (e: Exception) {
                Log.e("Map", "Failed to load bumps", e)
            }
        }
    }
}