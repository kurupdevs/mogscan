package com.kurupdevs.moggr.util

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * App lock: marks the session locked when the app leaves the foreground.
 * Call [isLocked] on resume and gate protected screens behind [unlock].
 *
 * The unlocked flag lives only in memory — nothing is persisted to disk,
 * so a fresh process always starts locked.
 */
object AppLock {

    @Volatile
    private var locked = false

    private var initialized = false

    /**
     * Call once from Application.onCreate. Registers a ProcessLifecycleOwner
     * observer that locks the app on ON_STOP (app backgrounded).
     */
    fun init(app: Application) {
        if (initialized) return
        initialized = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    locked = true
                }
            }
        )
    }

    /** True if the app must be re-authenticated before showing content. */
    fun isLocked(): Boolean = locked

    /**
     * Shows the system biometric/device-credential prompt. On success the
     * session is unlocked (in-memory only) and [onResult] is called with true;
     * any failure or cancellation delivers false. Callback runs on the main thread.
     */
    fun unlock(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        fun deliver(result: Boolean) {
            mainHandler.post { onResult(result) }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                locked = false
                deliver(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                deliver(false)
            }

            override fun onAuthenticationFailed() {
                // Transient failure (fingerprint not recognized) — don't dismiss the flow.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Moggr")
            .setSubtitle("Verify it's you to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
