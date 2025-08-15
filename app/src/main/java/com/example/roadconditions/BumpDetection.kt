package com.example.roadconditions

import android.Manifest
import android.app.*
import android.content.Context
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BumpDetection : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var accelerometer: Sensor? = null
    private var lastBumpTime = 0L
    private val bumpCooldown = 2000L

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            stopSelf()
        }

        startForeground(1, createNotification())
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.create().apply {
                interval = 1000
                fastestInterval = 500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

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

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val z = event?.values?.get(2) ?: return
        val verticalAcceleration = kotlin.math.abs(z - SensorManager.GRAVITY_EARTH)

        if (verticalAcceleration > 15.0f && System.currentTimeMillis() - lastBumpTime > bumpCooldown) {
            lastBumpTime = System.currentTimeMillis()
            showBumpToast()
            bumpNotification()
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
                .setContentTitle("Bump deteced!")
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
