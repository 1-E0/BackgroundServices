package test1.c14240126.backgroundservices

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isLatihanMode = false
    private var stepCount = 0
    private var isHigh = false

    private val NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "tracker_service_running"
    private val NOTIFICATION_CHANNEL_ID_JUMP_DETECTED = "tracker_jump_detected"
    private val NOTIFICATION_ID_SERVICE_RUNNING = 1
    private val NOTIFICATION_ID_JUMP_DETECTED = 2

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
        Log.d("TrackerService", "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isLatihanMode = intent?.getStringExtra("MODE") == "LATIHAN"
        Log.d("TrackerService", "Service onStartCommand, isLatihanMode: $isLatihanMode")
        startForeground(
            NOTIFICATION_ID_SERVICE_RUNNING,
            buildServiceNotification()
        )
        startJumpDetection()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("TrackerService", "Service onDestroy")
    }

    private fun startJumpDetection() {
        accelerometer?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun buildServiceNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isLatihanMode) "Latihan Berjalan" else "pelacak aktivasi latar"
        val text = if (isLatihanMode) "Latihan mulai berjalan..." else "pelacak tracker service sedang berjalan"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showJumlNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SERVICE_STOPPED_JUMP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingMainActivityIntent = PendingIntent.getActivity(
            this,
            1,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val jumpNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("lompat terdeteksi")
            .setContentText("service berhenti ketuk untuk membuka aplikasi")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED, jumpNotification)
        } catch (e: SecurityException) {
            Log.e("TrackerService", "gagal memunculkan notifikasi karena permission post notification tidak aktif ${e.message}", e)
        }
    }

    private fun showLatihanCompletedNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SERVICE_STOPPED_LATIHAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingMainActivityIntent = PendingIntent.getActivity(
            this,
            2,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val latihanNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("Latihan Selesai")
            .setContentText("Anda telah berjalan lebih dari 2 langkah")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED + 1, latihanNotification)
        } catch (e: SecurityException) {
            Log.e("TrackerService", "gagal memunculkan notifikasi latihan: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING,
                "Service Running",
                NotificationManager.IMPORTANCE_LOW
            )

            val jumpChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_JUMP_DETECTED,
                "Jump Detected",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(jumpChannel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]

            if (isLatihanMode) {
                val threshold = 12.0
                if (abs(y) > threshold) {
                    if (!isHigh) {
                        isHigh = true
                        stepCount++
                        Log.d("TrackerService", "Langkah latihan terdeteksi! Step: $stepCount, y: $y")
                        if (stepCount > 2) {
                            Log.d("TrackerService", "Latihan selesai! Lebih dari 2 langkah.")
                            sensorManager.unregisterListener(this)
                            showLatihanCompletedNotification()
                            stopSelf()
                        }
                    }
                } else {
                    if (abs(y) < 5.0) {
                        isHigh = false
                    }
                }
            } else {
                val jumpThresholds = 15.0
                if (abs(y) > jumpThresholds) {
                    Log.d("TrackerService", "lompatan terdeteksi! y: $y")
                    sensorManager.unregisterListener(this)
                    showJumlNotification()
                    stopSelf()
                }
            }
        }
    }
}
