package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker // [1] Tambah import ThirdWorker

class MainActivity : AppCompatActivity() {

    // Create an instance of a work manager
    // Work manager manages all your requests and workers
    // it also sets up the sequence for all your processes
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request notification permission for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // Create a constraint of which your workers are bound to.
        // Here the workers cannot execute the given process if
        // there's no internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001" // ID ini digunakan untuk semua worker dan notification channel 1

        // Create a one time work request for FirstWorker
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // Create a one time work request for SecondWorker
        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // [2] Create a one time work request for ThirdWorker
        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // [3] Sets up the process sequence: First -> Second -> Third
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest) // TAMBAHKAN ThirdWorker ke sequence
            .enqueue()

        // Observe the first worker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // Observe the second worker, then launch NotificationService 1
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    launchNotificationService() // Panggil Notif Service 1 setelah Worker 2
                }
            }

        // [4] Observe the third worker, then launch SecondNotificationService
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    launchSecondNotificationService() // Panggil Notif Service 2 setelah Worker 3
                }
            }
    }

    // Build the data into the correct format before passing it to the worker as input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Launch the NotificationService (Service 1)
    private fun launchNotificationService() {
        // Observe if the service process is done or not
        // If it is, show a toast with the channel ID in it
        NotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        // Create an Intent to start the NotificationService
        // An ID of "001" is also passed as the notification channel ID
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }

        // Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // [5] Launch the SecondNotificationService (Service 2)
    private fun launchSecondNotificationService() {
        // Observe if the service process is done or not (Menggunakan LiveData dari SecondNotificationService)
        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Second Notification Channel ID $Id is done!")
        }

        // Create an Intent to start the SecondNotificationService
        // An ID of "002" digunakan sebagai notification channel ID
        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002") // ID 002 untuk service kedua
        }

        // Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}