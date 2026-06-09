package br.com.zenith.ui.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ZenithNotificationType {
    Success,
    Error,
    Warning,
    Info
}

data class ZenithNotification(
    val message: String,
    val type: ZenithNotificationType
)

object ZenithNotifier {
    private val _notifications = MutableSharedFlow<ZenithNotification>(
        extraBufferCapacity = 8
    )
    val notifications = _notifications.asSharedFlow()

    fun success(message: String) {
        notify(message, ZenithNotificationType.Success)
    }

    fun error(message: String) {
        notify(message, ZenithNotificationType.Error)
    }

    fun warning(message: String) {
        notify(message, ZenithNotificationType.Warning)
    }

    fun info(message: String) {
        notify(message, ZenithNotificationType.Info)
    }

    private fun notify(message: String, type: ZenithNotificationType) {
        _notifications.tryEmit(ZenithNotification(message = message, type = type))
    }
}
