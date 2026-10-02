package com.kurupdevs.moggr

import android.app.Application
import com.kurupdevs.moggr.util.AppLock
import com.kurupdevs.moggr.util.TempFileJanitor

/**
 * Application entry point for Moggr.
 *
 * Named MoggrApplication (not MoggrApp) because MainActivity.kt already declares
 * a @Composable named MoggrApp in this package.
 *
 * Register in AndroidManifest.xml:
 *   <application android:name=".MoggrApplication" ...>
 */
class MoggrApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        TempFileJanitor.prune(this)
        AppLock.init(this)
    }
}
