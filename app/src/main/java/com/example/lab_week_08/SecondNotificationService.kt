package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {
    // Second service for the assignment, triggered after ThirdWorker completion

    // Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder
    // Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    // This is used to bind a two-way communication
    // In this tutorial, we will only be using a one-way communication
    // therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    // This is a callback and part of the life cycle
    // the onCreate callback will be called when this service
    // is created for the first time
    override fun onCreate() {
        super.onCreate()

        // Create the notification with all of its contents and configurations
        // in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        // Create the handler to control which thread the notification will be executed on.
        // Instantiating a new HandlerThread for background execution
        val handlerThread = HandlerThread("ThirdThread").apply { start() } // Renamed thread for uniqueness
        serviceHandler = Handler(handlerThread.looper)
    }

    // Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        // Create a pending Intent which is used to be executed when the user clicks the notification
        val pendingIntent = getPendingIntent()

        // To make a notification, a channel is required
        // Using a different Channel ID: "002"
        val channelId = createNotificationChannel()

        // Combine both the pending Intent and the channel into a notification builder
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        // Start the foreground service and the notification will appear on the user's device
        // Using a different NOTIFICATION_ID: 0xCA8
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    // A pending Intent is the Intent used to be executed when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        // Checking for SDK version for Flag
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        // Setting MainActivity into the pending Intent
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    // Using Channel ID and Channel Name "002"
    private fun createNotificationChannel(): String =
        // Notification channel exists only for API 26 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the unique channel id
            val channelId = "002"
            // Create the unique channel name
            val channelName = "002 Channel"
            // Create the channel priority
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            // Build the channel notification
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            // Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(
                    this,
                    NotificationManager::class.java
                )
            )
            // Binds the channel into the NotificationManager
            service.createNotificationChannel(channel)
            // Return the channel id
            channelId
        } else { "" }

    // Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ) =
        NotificationCompat.Builder(this, channelId)
            // Sets the title for Third Worker
            .setContentTitle("Third worker process is done")
            // Sets the content for countdown
            .setContentText("Countdown for second service!")
            // Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // Sets the action/intent to be executed when the user clicks the notification
            .setContentIntent(pendingIntent)
            // Sets the ticker message
            .setTicker("Third worker process is done, launching second service!")
            // setOnGoing() controls whether the notification is dismissible or not by the user
            .setOngoing(true)

    // This is a callback and part of a life cycle
    // This callback will be called when the service is started
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Posts the notification task to the handler, which will be executed on a different thread
        serviceHandler.post {
            // Counting down from 5 to 0 to avoid toast collisions
            countDownFromTenToZero(notificationBuilder, 5)

            // Notifying the MainActivity that the service process is done
            notifyCompletion(Id)
            // Stops the foreground service
            stopForeground(STOP_FOREGROUND_REMOVE)
            // Stop and destroy the service
            stopSelf()
        }
        return returnValue
    }

    // A function to update the notification to display a count down
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder, seconds: Int) {
        // Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Count down from the provided seconds to 0
        for (i in seconds downTo 0) {
            Thread.sleep(1000L)
            // Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            // Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    // Update the unique LiveData with the returned channel id through the Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            // Using the unique LiveData for the second service
            mutableID_Second.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8 // Unique ID for the second service
        const val EXTRA_ID = "Id"

        // Unique LiveData which is a data holder that automatically updates the UI
        private val mutableID_Second = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID_Second
    }
}