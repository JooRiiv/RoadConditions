package com.example.roadconditions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { nonNullIntent ->
            if (ActivityRecognitionResult.hasResult(nonNullIntent)) {
                val result = ActivityRecognitionResult.extractResult(nonNullIntent) ?: return
                val activity = result.mostProbableActivity
                val type = activity.type
                val confidence = activity.confidence

                when (type) {
                    DetectedActivity.IN_VEHICLE -> {
                        context?.let {
                            val localIntent = Intent("activity_in_vehicle_detected")
                            localIntent.putExtra("confidence", confidence)
                            LocalBroadcastManager.getInstance(it).sendBroadcast(localIntent)
                        }
                    }

                    DetectedActivity.STILL -> {
                        context?.let {
                            val localIntent = Intent("activity_vehicle_exit_detected")
                            localIntent.putExtra("confidence", confidence)
                            LocalBroadcastManager.getInstance(it).sendBroadcast(localIntent)
                        }
                    }
                }
            }
        }
    }
}
