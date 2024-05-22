package com.mybgapp.backgroundgeolocation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.location.LocationServices
import com.mybgapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext, LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START -> start(intent)
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun start(intent: Intent) {
        val interval = intent.extras!!.getInt("interval")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking location")
            .setContentText("Your location is being tracked in the background!")
            .setSmallIcon(R.mipmap.ic_launcher_round).setOngoing(true)

        locationClient.getLocationUpdates(interval.toLong()).catch { e -> e.printStackTrace() }
            .onEach { location ->
                val payload = Arguments.createMap()
                payload.putString("latitude", location.latitude.toString())
                payload.putString("longitude", location.longitude.toString())
                if (BackgroundGeolocationModule.context != null) {
                    BackgroundGeolocationModule.context!!.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onLocation", payload)
                }
            }.launchIn(serviceScope)

        startForeground(FOREGROUND_TASK_ID, notification.build())
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val FOREGROUND_TASK_ID = 2003400
        private const val NOTIFICATION_CHANNEL_ID = "CHANNEL_BG_LOCATION"
        private const val NOTIFICATION_CHANNEL_NAME = "CHANNEL_NAME_BG_LOCATION"
    }
}