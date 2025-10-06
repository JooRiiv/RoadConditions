package com.example.roadconditions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    private fun sendDrivingStatus(context: Context, isDriving: Boolean) {
        val intent = Intent("com.example.roadconditions.DRIVING_STATUS")
        intent.putExtra("isDriving", isDriving)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (intent.action != "com.example.roadconditions.ACTION_ACTIVITY_TRANSITION") return

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return

            for (event in result.transitionEvents) {
                if (event.activityType == DetectedActivity.IN_VEHICLE) {
                    when (event.transitionType) {
                        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                            Toast.makeText(context, "User is driving", Toast.LENGTH_SHORT).show()
                                sendDrivingStatus(context!!, true)
                        }
                        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                            sendDrivingStatus(context!!, false)
                        }
                    }
                }
            }
        } else {
            Log.d("ActivityRecognitionReceiver", "No transition result found")
        }
    }
}