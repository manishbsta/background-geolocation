package com.mybgapp.backgroundgeolocation

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
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

    init {
        context = this.reactApplicationContext
    }

    override fun getName(): String {
        return "BackgroundGeolocation"
    }

    @ReactMethod
    fun start(intervalInSecs: Int) {
        if (!isRunning()) {
            val locationManager =
                reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!isGpsEnabled) {
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                reactApplicationContext.startActivity(intent)

                enableGps()
            }

            val intervalInMs = intervalInSecs * 1000;
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

    private fun enableGps() {
        // Create a LocationRequest with high accuracy
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // Build a LocationSettingsRequest
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        // Check the state of the location settings
        val client: SettingsClient = LocationServices.getSettingsClient(reactApplicationContext)
        val task = client.checkLocationSettings(builder.build())

        // Prompt the user to change location settings
        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied, GPS is already enabled
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                    exception.startResolutionForResult(currentActivity!!, 123)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            }
        }
    }


    companion object {
        private const val SHORTEST_INTERVAL = 10 * 1000 // 10 secs
        var context: ReactApplicationContext? = null
    }
}
