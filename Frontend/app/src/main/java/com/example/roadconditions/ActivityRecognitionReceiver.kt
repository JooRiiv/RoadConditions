package com.example.roadconditions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null) return
        if (!ActivityRecognitionResult.hasResult(intent)) return

        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val activities = result.probableActivities

        var inVehicleDetected = false

        for (activity in activities) {
            Log.d("ActivityRecognition", "Detected: ${activity.type}, Confidence: ${activity.confidence}")
            if (activity.type == DetectedActivity.IN_VEHICLE && activity.confidence >= 35) {
                inVehicleDetected = true
                val serviceIntent = Intent(context, BumpDetection::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
                break
            }
        }

        if (!inVehicleDetected) {
            context.stopService(Intent(context, BumpDetection::class.java))
        }

        val uiIntent = Intent("vehicle_state_changed")
        uiIntent.putExtra("inVehicle", inVehicleDetected)
        LocalBroadcastManager.getInstance(context).sendBroadcast(uiIntent)
    }
}

