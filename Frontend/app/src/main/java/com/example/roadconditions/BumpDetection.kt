package com.example.roadconditions

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs


class BumpDetection : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var accelerometer: Sensor? = null
    private var lastBumpTime = 0L
    private val bumpCooldown = 5000L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    object BumpClient {
        private const val ENDPOINT_URL =
            "https://roadconditions.yellowglacier-a8220dfb.norwayeast.azurecontainerapps.io/bumps"
        private val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
        suspend fun postBump(bump: Bump) {
            client.post(ENDPOINT_URL) {
                contentType(ContentType.Application.Json)
                setBody(bump)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onSensorChanged(event: SensorEvent?) {
        val z = event?.values?.get(2) ?: return
        val verticalAcceleration = abs(z - SensorManager.GRAVITY_EARTH)

        if (verticalAcceleration > 15.0f && System.currentTimeMillis() - lastBumpTime > bumpCooldown) {
            lastBumpTime = System.currentTimeMillis()
            showBumpToast()
            bumpNotification()

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val timestamp =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date())

                    val signalType =
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            "Precise"
                        } else {
                            "Coarse"
                        }

                    val bump = Bump(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = timestamp,
                        signal = signalType
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            BumpClient.postBump(bump)
                            Log.i("BumpDetection", "Posted bump successfully")
                        } catch (e: Exception) {
                            Log.e("BumpDetection", "Failed to post bump", e)
                        }
                    }
                }
            }

        }
    }

    private fun showBumpToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Bump detected", Toast.LENGTH_SHORT).show()
        }
    }

        fun bumpNotification() {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = formatter.format(Date())
            val builder = NotificationCompat.Builder(this, "bump_service_channel")
                .setSmallIcon(R.drawable.bumpnotificationicon)
                .setContentTitle("Bump detected!")
                .setContentText("Bump detected at $currentTime.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    with(NotificationManagerCompat.from(this)) {
                        notify(1001, builder.build())
                    }
                }
            } else {
                with(NotificationManagerCompat.from(this)) {
                    notify(1001, builder.build())
                }
            }
        }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "bump_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bump Detection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bump Detection Active")
            .setContentText("Monitoring for road bumps...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }
}
