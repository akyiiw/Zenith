package br.com.zenith.service

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
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import br.com.zenith.MainActivity
import br.com.zenith.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

class ActivityTrackingService : Service(), SensorEventListener {

    inner class LocalBinder : Binder() {
        fun getService(): ActivityTrackingService = this@ActivityTrackingService
    }

    private val binder = LocalBinder()

    // --- Estado público ---
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _status = MutableStateFlow(TrackingStatus.IDLE)
    val status: StateFlow<TrackingStatus> = _status

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _distanceMeters = MutableStateFlow(0f)
    val distanceMeters: StateFlow<Float> = _distanceMeters

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    // --- Internals ---
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Passos
    private var sensorManager: SensorManager? = null
    private var initialStepCount = -1

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLatLng: LatLng? = null
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 5000L
    ).setMinUpdateDistanceMeters(5f).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_status.value != TrackingStatus.RUNNING) return
            val location = result.lastLocation ?: return
            val newPoint = LatLng(location.latitude, location.longitude)

            lastLatLng?.let { last ->
                _distanceMeters.value += haversine(last, newPoint)
            }
            lastLatLng = newPoint
            _routePoints.value += newPoint
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        _elapsedSeconds.value = 0L
        _steps.value = 0
        _distanceMeters.value = 0f
        _routePoints.value = emptyList()
        lastLatLng = null
        initialStepCount = -1
        _status.value = TrackingStatus.RUNNING

        startForeground(NOTIFICATION_ID, buildNotification())
        startTimer()
        startLocationUpdates()
        startStepCounter()
    }

    private fun pause() {
        _status.value = TrackingStatus.PAUSED
        timerJob?.cancel()
        stopLocationUpdates()
        stopStepCounter()
        updateNotification()
    }

    private fun resume() {
        _status.value = TrackingStatus.RUNNING
        startTimer()
        startLocationUpdates()
        startStepCounter()
        updateNotification()
    }

    private fun stop() {
        timerJob?.cancel()
        stopLocationUpdates()
        stopStepCounter()
        _status.value = TrackingStatus.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Timer ---
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                updateNotification()
            }
        }
    }

    // --- GPS ---
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permissão não concedida
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // --- Passos ---
    private fun startStepCounter() {
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopStepCounter() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        if (_status.value != TrackingStatus.RUNNING) return

        val totalSteps = event.values[0].toInt()
        if (initialStepCount == -1) {
            initialStepCount = totalSteps
        }
        _steps.value = totalSteps - initialStepCount
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Notificação ---
    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Atividade em andamento",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val time = formatTime(_elapsedSeconds.value)
        val km = "%.2f km".format(_distanceMeters.value / 1000f)
        val passos = "${_steps.value} passos"
        val statusLabel = if (_status.value == TrackingStatus.PAUSED) " · Pausado" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zenith · Atividade em andamento$statusLabel")
            .setContentText("$time · $km · $passos")
            .setSmallIcon(R.mipmap.zenith_wg)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    // --- Haversine (distância entre dois pontos GPS) ---
    private fun haversine(a: LatLng, b: LatLng): Float {
        val r = 6371000f // raio da Terra em metros
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return (2 * r * asin(sqrt(h))).toFloat()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "zenith_tracking"
        const val NOTIFICATION_ID = 1001

        fun formatTime(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
            else "%02d:%02d".format(m, s)
        }
    }
}

enum class TrackingStatus { IDLE, RUNNING, PAUSED, STOPPED }