package test1.c14240126.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "test1.c14240126.backgroundservices2.ACTION_SERVICE_STOPPED_JUMP"
        const val ACTION_SERVICE_STOPPED_LATIHAN = "test1.c14240126.backgroundservices2.ACTION_SERVICE_STOPPED_LATIHAN"
        const val REQUEST_CODE_PERMISSIONS = 101
    }

    private lateinit var statusTextView: TextView
    private lateinit var btnStart: Button
    private lateinit var btnLatihan: Button

    private val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        btnStart = findViewById(R.id.btnStart)
        btnLatihan = findViewById(R.id.btnLatihan)

        handleIntent(intent)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnLatihan.setOnClickListener {
            if (hasAllPermissions()) {
                val intent = Intent(this, TrackerService::class.java).apply {
                    putExtra("MODE", "LATIHAN")
                }
                startForegroundService(intent)
                statusTextView.text = "Latihan sedang berjalan..."
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP) {
            statusTextView.text = "service berhenti karena anda lompat"
        } else if (intent?.action == ACTION_SERVICE_STOPPED_LATIHAN) {
            val stepCount = intent.getIntExtra("STEP_COUNT", 0)
            statusTextView.text = "Latihan selesai! Jumlah langkah: $stepCount"
            createCalendarEvent(stepCount)
        }
    }

    private fun createCalendarEvent(stepCount: Int) {
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Latihan hari ini")
            putExtra(CalendarContract.Events.DESCRIPTION, "total langkah latihan hari ini: $stepCount")
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis())
        }
        try {
            startActivity(calendarIntent)
        } catch (e: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                val serviceIntent = Intent(this, TrackerService::class.java)
                startForegroundService(serviceIntent)
                finish()
            } else {
                statusTextView.text = "izin ditolak"
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return permissionToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService() {
        val intent = Intent(this, TrackerService::class.java)
        startForegroundService(intent)
        statusTextView.text = "background service sedang berjalan,,,,"
    }
}
