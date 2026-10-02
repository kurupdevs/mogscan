package com.kurupdevs.moggr.util

import android.content.Context
import java.io.File

/**
 * Best-effort sweep for stale temp files the capture flow may have left behind
 * (e.g. if the process died between capture and the onImageSaved cleanup).
 *
 * Deletes:
 * - moggr_*.jpg files directly under cacheDir that are older than 24 hours
 * - any files under cacheDir/shared/ that are older than 7 days
 *
 * Never throws. Call once on app start (off the main thread).
 */
object TempFileJanitor {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun prune(context: Context) {
        try {
            val cacheDir = context.cacheDir ?: return
            val now = System.currentTimeMillis()

            // Stale capture temp files.
            val cached = try {
                cacheDir.listFiles()
            } catch (_: Exception) {
                null
            } ?: emptyArray()
            for (f in cached) {
                try {
                    if (f.isFile
                        && f.name.startsWith("moggr_")
                        && f.name.endsWith(".jpg")
                        && now - f.lastModified() > DAY_MS
                    ) {
                        f.delete()
                    }
                } catch (_: Exception) {
                    // keep sweeping the rest
                }
            }

            // Old shared/exported files, if the dir exists.
            val sharedDir = File(cacheDir, "shared")
            if (sharedDir.isDirectory) {
                val shared = try {
                    sharedDir.listFiles()
                } catch (_: Exception) {
                    null
                } ?: emptyArray()
                for (f in shared) {
                    try {
                        if (now - f.lastModified() > 7 * DAY_MS) {
                            if (f.isDirectory) f.deleteRecursively() else f.delete()
                        }
                    } catch (_: Exception) {
                        // keep sweeping the rest
                    }
                }
            }
        } catch (_: Exception) {
            // never throw from the janitor
        }
    }
}
