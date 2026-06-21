package br.com.zenith.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import br.com.zenith.MainActivity
import br.com.zenith.R
import br.com.zenith.utils.UnitFormatters
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

    private val _rawDistanceMeters = MutableStateFlow(0f)
    val rawDistanceMeters: StateFlow<Float> = _rawDistanceMeters

    private val _acceptedGpsPoints = MutableStateFlow(0)
    val acceptedGpsPoints: StateFlow<Int> = _acceptedGpsPoints

    private val _rejectedGpsPoints = MutableStateFlow(0)
    val rejectedGpsPoints: StateFlow<Int> = _rejectedGpsPoints

    private val _averageAccuracyMeters = MutableStateFlow(0f)
    val averageAccuracyMeters: StateFlow<Float> = _averageAccuracyMeters

    private val _gpsQuality = MutableStateFlow("ruim")
    val gpsQuality: StateFlow<String> = _gpsQuality

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    // --- Internals ---
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Passos
    private var sensorManager: SensorManager? = null
    private var accumulatedSteps = 0
    private var lastStepCounterValue = -1
    private var resetStepBaselineOnNextEvent = true

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLatLng: LatLng? = null
    private var lastRawLatLng: LatLng? = null
    private var lastAcceptedLocation: Location? = null
    private var lastRouteLocation: Location? = null
    private var gpsDistanceMeters = 0f
    private var accuracySum = 0f
    private var wakeLock: PowerManager.WakeLock? = null
    private var targetDistanceMeters: Float? = null
    private var targetSeconds: Long? = null
    private var exerciseKind: String = "corrida"
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000L
    )
        .setMinUpdateIntervalMillis(1000L)
        .setMaxUpdateDelayMillis(0L)
        .setMinUpdateDistanceMeters(2f)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_status.value != TrackingStatus.RUNNING) return
            result.locations
                .sortedWith(compareBy<Location> { it.elapsedRealtimeNanos }.thenBy { it.time })
                .forEach(::handleLocation)
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
            ACTION_START -> start(intent)
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start(intent: Intent?) {
        exerciseKind = intent?.getStringExtra(EXTRA_EXERCISE_KIND) ?: "corrida"
        targetDistanceMeters = intent?.getFloatExtra(EXTRA_TARGET_DISTANCE_METERS, -1f)
            ?.takeIf { it > 0f }
        targetSeconds = intent?.getLongExtra(EXTRA_TARGET_SECONDS, -1L)
            ?.takeIf { it > 0L }
        _elapsedSeconds.value = 0L
        _steps.value = 0
        _distanceMeters.value = 0f
        _rawDistanceMeters.value = 0f
        _acceptedGpsPoints.value = 0
        _rejectedGpsPoints.value = 0
        _averageAccuracyMeters.value = 0f
        _gpsQuality.value = "ruim"
        _routePoints.value = emptyList()
        lastLatLng = null
        lastRawLatLng = null
        lastAcceptedLocation = null
        lastRouteLocation = null
        accuracySum = 0f
        accumulatedSteps = 0
        lastStepCounterValue = -1
        resetStepBaselineOnNextEvent = true
        gpsDistanceMeters = 0f
        _status.value = TrackingStatus.RUNNING

        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )
        acquireWakeLock()
        startTimer()
        startLocationUpdates()
        startStepCounter()
    }

    private fun pause() {
        _status.value = TrackingStatus.PAUSED
        timerJob?.cancel()
        stopLocationUpdates()
        stopStepCounter()
        releaseWakeLock()
        updateNotification()
    }

    private fun resume() {
        _status.value = TrackingStatus.RUNNING
        acquireWakeLock()
        startTimer()
        startLocationUpdates()
        startStepCounter()
        updateNotification()
    }

    private fun stop() {
        timerJob?.cancel()
        stopLocationUpdates()
        stopStepCounter()
        releaseWakeLock()
        _status.value = TrackingStatus.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    // --- Timer ---
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                targetSeconds?.let { target ->
                    if (_elapsedSeconds.value >= target) stop()
                }
                updateNotification()
            }
        }
    }

    private fun isUsableLocation(location: Location): Boolean {
        return location.hasAccuracy() && location.accuracy <= MAX_ACCEPTABLE_ACCURACY_METERS
    }

    private fun handleLocation(location: Location) {
        val newPoint = LatLng(location.latitude, location.longitude)

        lastRawLatLng?.let { last ->
            _rawDistanceMeters.value += haversine(last, newPoint)
        }
        lastRawLatLng = newPoint

        if (!isUsableLocation(location)) {
            rejectPoint()
            return
        }

        val lastAccepted = lastAcceptedLocation
        if (lastAccepted == null) {
            lastAcceptedLocation = location
            acceptRoutePoint(location, newPoint)
            updateDisplayedDistance()
            return
        }

        val distance = location.distanceTo(lastAccepted)
        val seconds = ((location.time - lastAccepted.time).coerceAtLeast(1000L) / 1000f)
        val speed = distance / seconds
        val noiseFloor = max(MIN_GPS_DISTANCE_METERS, ((location.accuracy + lastAccepted.accuracy) / 2f) * 0.35f)

        if (speed > maxSpeedForExercise()) {
            rejectPoint()
            return
        }

        maybeAcceptRoutePoint(location, newPoint)

        if (distance >= noiseFloor) {
            gpsDistanceMeters += distance
            lastAcceptedLocation = location
        }

        updateDisplayedDistance()

        targetDistanceMeters?.let { target ->
            if (_distanceMeters.value >= target) stop()
        }
    }

    private fun maybeAcceptRoutePoint(location: Location, point: LatLng) {
        val lastRoute = lastRouteLocation
        if (lastRoute == null || location.distanceTo(lastRoute) >= ROUTE_MIN_DISTANCE_METERS) {
            acceptRoutePoint(location, point)
        }
    }

    private fun acceptRoutePoint(location: Location, point: LatLng) {
        lastRouteLocation = location
        lastLatLng = point
        _routePoints.value += point
        _acceptedGpsPoints.value += 1
        accuracySum += location.accuracy
        _averageAccuracyMeters.value = accuracySum / _acceptedGpsPoints.value.coerceAtLeast(1)
        updateGpsQuality()
    }

    private fun rejectPoint() {
        _rejectedGpsPoints.value += 1
        updateGpsQuality()
    }

    private fun updateGpsQuality() {
        val accepted = _acceptedGpsPoints.value
        val rejected = _rejectedGpsPoints.value
        val total = accepted + rejected
        val rejectionRate = if (total == 0) 1f else rejected.toFloat() / total
        _gpsQuality.value = when {
            accepted < 4 || _averageAccuracyMeters.value > 25f || rejectionRate > 0.4f -> "ruim"
            _averageAccuracyMeters.value > 15f || rejectionRate > 0.2f -> "media"
            else -> "boa"
        }
    }

    private fun maxSpeedForExercise(): Float {
        return when (exerciseKind.lowercase()) {
            "caminhada" -> 4.2f
            "ciclismo" -> 22f
            else -> 9.5f
        }
    }

    private fun supportsStepDistance(): Boolean {
        val kind = exerciseKind.lowercase()
        return kind.contains("caminh") || kind.contains("corr") || kind.contains("run") || kind.contains("walk")
    }

    private fun estimatedStepDistanceMeters(): Float {
        if (!supportsStepDistance()) return 0f
        val stepLength = when {
            exerciseKind.lowercase().contains("corr") || exerciseKind.lowercase().contains("run") -> 1.0f
            else -> 0.8f
        }
        return _steps.value * stepLength
    }

    private fun updateDisplayedDistance() {
        val stepDistance = estimatedStepDistanceMeters()
        val accepted = _acceptedGpsPoints.value
        val rejected = _rejectedGpsPoints.value
        val total = accepted + rejected
        val rejectionRate = if (total == 0) 0f else rejected.toFloat() / total
        val gpsTooLowComparedToSteps = stepDistance >= 250f && gpsDistanceMeters < stepDistance * 0.45f
        val shouldUseStepFallback = supportsStepDistance() &&
            stepDistance > gpsDistanceMeters &&
            (accepted < 4 || rejectionRate > 0.45f || gpsTooLowComparedToSteps)

        _distanceMeters.value = if (shouldUseStepFallback) {
            max(gpsDistanceMeters, stepDistance)
        } else {
            gpsDistanceMeters
        }
    }

    private fun acquireWakeLock() {
        val current = wakeLock
        if (current?.isHeld == true) return
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:ActivityTracking")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
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
            resetStepBaselineOnNextEvent = true
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
        if (lastStepCounterValue == -1 || resetStepBaselineOnNextEvent) {
            lastStepCounterValue = totalSteps
            resetStepBaselineOnNextEvent = false
            return
        }

        val delta = (totalSteps - lastStepCounterValue).coerceAtLeast(0)
        if (delta > 0) {
            accumulatedSteps += delta
            lastStepCounterValue = totalSteps
            _steps.value = accumulatedSteps
            updateDisplayedDistance()
        }
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
        val km = UnitFormatters.kilometersWithSpace(_distanceMeters.value / 1000.0)
        val passos = "${_steps.value} passos"
        val statusLabel = if (_status.value == TrackingStatus.PAUSED) " · Pausado" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zenith · Atividade em andamento$statusLabel")
            .setContentText("$time · $km · $passos")
            .setSmallIcon(R.drawable.zenith)
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
        const val EXTRA_EXERCISE_KIND = "EXTRA_EXERCISE_KIND"
        const val EXTRA_TARGET_DISTANCE_METERS = "EXTRA_TARGET_DISTANCE_METERS"
        const val EXTRA_TARGET_SECONDS = "EXTRA_TARGET_SECONDS"
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 60f
        private const val MIN_GPS_DISTANCE_METERS = 3f
        private const val ROUTE_MIN_DISTANCE_METERS = 3f
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
