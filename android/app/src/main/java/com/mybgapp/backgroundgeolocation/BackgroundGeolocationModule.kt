package com.mybgapp.backgroundgeolocation

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.util.Log
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient

class BackgroundGeolocationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "BackgroundGeolocation"
    }

    @ReactMethod
    fun start(intervalInSecs: Int) {
        if (!isRunning()) {
            INTERVAL_IN_SECS = intervalInSecs

            if (!isGpsEnabled()) {
                enableGps()
            } else {
                startService()
            }
        }
    }

    @ReactMethod
    fun stop() {
        Intent(reactApplicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            reactApplicationContext.startService(this)
        }
    }

    @ReactMethod
    fun isRunning(): Boolean {
        val am = reactApplicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (info in am.getRunningServices(Int.MAX_VALUE)) {
            if (info.service.className == LocationService::class.java.name) {
                return true
            }
        }

        return false
    }

    @ReactMethod
    fun isGpsEnabled(): Boolean {
        val locationManager =
            reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        return isGpsEnabled
    }

    private fun enableGps() {
        // Create a LocationRequest with high accuracy
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, (SHORTEST_INTERVAL * 1000).toLong()
        ).build()

        // Build a LocationSettingsRequest
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        // Check the state of the location settings
        val client: SettingsClient = LocationServices.getSettingsClient(reactApplicationContext)
        val task = client.checkLocationSettings(builder.build())

        // Prompt the user to change location settings
        task.addOnSuccessListener {
            // All location settings are satisfied, GPS is already enabled
            startService()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                    exception.startResolutionForResult(
                        this.currentActivity!!, LOCATION_SERVICE_REQUEST_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            }
        }
    }

    private val activityEventListener = object : BaseActivityEventListener() {
        override fun onActivityResult(
            activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?
        ) {
            if (requestCode == LOCATION_SERVICE_REQUEST_CODE) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        startService()
                        Log.d("LocationService", "RESULT_OK")
                    }

                    else -> Log.d("LocationService", "!RESULT_OK")
                }
            }

            super.onActivityResult(activity, requestCode, resultCode, data)
        }
    }

    private fun startService() {
        val intervalInMs = INTERVAL_IN_SECS * 1000
        Intent(reactApplicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            if (intervalInMs >= SHORTEST_INTERVAL) {
                this.putExtra("interval", intervalInMs)
            } else {
                this.putExtra("interval", SHORTEST_INTERVAL)
            }

            reactApplicationContext.startService(this)
        }
    }


    init {
        context = this.reactApplicationContext
        reactContext.addActivityEventListener(activityEventListener)
    }

    companion object {
        private const val LOCATION_SERVICE_REQUEST_CODE = 999
        private const val SHORTEST_INTERVAL = 10 * 1000 // 10 secs
        private var INTERVAL_IN_SECS = SHORTEST_INTERVAL
        var context: ReactApplicationContext? = null
    }
}
