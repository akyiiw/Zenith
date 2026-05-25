package br.com.zenith.viewmodels.activity

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.zenith.service.ActivityTrackingService
import br.com.zenith.service.TrackingStatus
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ActiveActivityViewModel(application: Application) : AndroidViewModel(application) {

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

    private var service: ActivityTrackingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as ActivityTrackingService.LocalBinder
            service = localBinder.getService()
            bound = true

            viewModelScope.launch {
                service!!.elapsedSeconds.collectLatest { _elapsedSeconds.value = it }
            }
            viewModelScope.launch {
                service!!.status.collectLatest { _status.value = it }
            }
            viewModelScope.launch {
                service!!.steps.collectLatest { _steps.value = it }
            }
            viewModelScope.launch {
                service!!.distanceMeters.collectLatest { _distanceMeters.value = it }
            }
            viewModelScope.launch {
                service!!.routePoints.collectLatest { _routePoints.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    fun bindAndStart(context: Context) {
        val intent = Intent(context, ActivityTrackingService::class.java).apply {
            action = ActivityTrackingService.ACTION_START
        }
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun pause(context: Context) {
        sendAction(context, ActivityTrackingService.ACTION_PAUSE)
    }

    fun resume(context: Context) {
        sendAction(context, ActivityTrackingService.ACTION_RESUME)
    }

    private fun sendAction(context: Context, action: String) {
        val intent = Intent(context, ActivityTrackingService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    private fun unbind(context: Context) {
        if (bound) {
            context.unbindService(connection)
            bound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
    }

    fun serializarRota(points: List<LatLng>): String {
        val sb = StringBuilder("[")
        points.forEachIndexed { i, p ->
            sb.append("{\"lat\":${p.latitude},\"lng\":${p.longitude}}")
            if (i < points.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    data class TrackingResult(
        val duracaoSeconds: Long,
        val steps: Int,
        val distanceMeters: Float,
        val rotaJson: String
    )

    fun stop(context: Context): TrackingResult {
        val result = TrackingResult(
            duracaoSeconds = _elapsedSeconds.value,
            steps = _steps.value,
            distanceMeters = _distanceMeters.value,
            rotaJson = serializarRota(_routePoints.value)
        )
        sendAction(context, ActivityTrackingService.ACTION_STOP)
        unbind(context)
        return result
    }
}