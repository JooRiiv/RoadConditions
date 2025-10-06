package com.example.roadconditions

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*

class ActivityRecognitionService : Service()  {

    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        activityRecognitionClient = ActivityRecognition.getClient(this)
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        requestActivityUpdates()
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onDestroy() {
        super.onDestroy()
        activityRecognitionClient.removeActivityTransitionUpdates(getPendingIntent())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun requestActivityUpdates() {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        activityRecognitionClient.requestActivityTransitionUpdates(
            request,
            getPendingIntent()
        ).addOnSuccessListener {
            Log.d("ActivityRecognitionService", "Activity updates registered")
        }.addOnFailureListener {
            Log.e("ActivityRecognitionService", "Failed: ${it.message}")
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityRecognitionReceiver::class.java).apply {
            action = "com.example.roadconditions.ACTION_ACTIVITY_TRANSITION"
        }
        return PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelId = "driving_channel"
        val channelName = "Driving Recognition"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Activity detection active")
            .setContentText("Monitoring if the user is driving.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }
}