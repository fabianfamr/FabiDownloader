package com.fabian.downloader.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

/**
 * Utility for managing Android Toast messages.
 * Prevents toast stacking/queueing by cancelling previous toasts,
 * debounces duplicate messages, and guarantees main-thread execution.
 */
object ToastUtils {

    private var lastToastRef: WeakReference<Toast>? = null
    private var lastMessage: String? = null
    private var lastShownTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private const val DEBOUNCE_INTERVAL_MS = 2000L

    @JvmStatic
    fun show(
        context: Context?,
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
        forceShow: Boolean = false
    ) {
        if (context == null || message.isBlank()) return

        val now = System.currentTimeMillis()

        // Debounce exact same message within DEBOUNCE_INTERVAL_MS unless forceShow is true
        if (!forceShow && message == lastMessage && (now - lastShownTime) < DEBOUNCE_INTERVAL_MS) {
            return
        }

        val appContext = context.applicationContext ?: context

        runOnMainThread {
            try {
                // Cancel previous toast so it doesn't queue up behind it
                lastToastRef?.get()?.cancel()

                val newToast = Toast.makeText(appContext, message, duration)
                lastToastRef = WeakReference(newToast)
                lastMessage = message
                lastShownTime = now

                newToast.show()
            } catch (e: Exception) {
                android.util.Log.e("ToastUtils", "Error showing toast: ${e.localizedMessage}")
            }
        }
    }

    @JvmStatic
    fun show(
        context: Context?,
        @StringRes resId: Int,
        duration: Int = Toast.LENGTH_SHORT,
        vararg formatArgs: Any
    ) {
        if (context == null) return
        val message = try {
            if (formatArgs.isNotEmpty()) {
                context.getString(resId, *formatArgs)
            } else {
                context.getString(resId)
            }
        } catch (e: Exception) {
            ""
        }
        show(context, message, duration)
    }

    @JvmStatic
    fun showShort(context: Context?, message: String) {
        show(context, message, Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun showShort(context: Context?, @StringRes resId: Int, vararg formatArgs: Any) {
        show(context, resId, Toast.LENGTH_SHORT, *formatArgs)
    }

    @JvmStatic
    fun showLong(context: Context?, message: String) {
        show(context, message, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun showLong(context: Context?, @StringRes resId: Int, vararg formatArgs: Any) {
        show(context, resId, Toast.LENGTH_LONG, *formatArgs)
    }

    @JvmStatic
    fun cancel() {
        runOnMainThread {
            try {
                lastToastRef?.get()?.cancel()
                lastToastRef = null
                lastMessage = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
