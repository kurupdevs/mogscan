package com.kurupdevs.moggr.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Verifies the app's signing certificate to detect tampered/repackaged installs.
 *
 * How it works: SHA-256 of the APK's signing certificate is compared against
 * [EXPECTED_SIG_SHA256]. If the app is re-signed by anyone other than kurupdevs,
 * the check fails.
 *
 * **SETUP — replace [EXPECTED_SIG_SHA256] before shipping:**
 * After generating the release keystore, run:
 * ```
 * keytool -list -v -keystore <release.keystore> -alias <alias> | grep SHA256
 * ```
 * Paste the resulting hex (uppercase, no colons) into EXPECTED_SIG_SHA256.
 * Until replaced, [isIntact] will return false for every build.
 */
object SignatureCheck {

    /**
     * ⚠️ REPLACE_ME — paste the release keystore's SHA-256 certificate fingerprint
     * here (uppercase hex, no colons). See KDoc above for how to obtain it.
     */
    const val EXPECTED_SIG_SHA256 = "F21D3537E42CDD8C17AE2124CD7C4C6C3DDE4D2455BC4EE7924E0D68FBC87622"

    private const val PACKAGE_NAME = "com.kurupdevs.moggr"

    /**
     * Returns true only if the package name matches and at least one APK signer
     * certificate matches [EXPECTED_SIG_SHA256]. Never throws — any failure
     * returns false (fail-closed).
     */
    fun isIntact(context: Context): Boolean {
        return try {
            if (context.packageName != PACKAGE_NAME) return false

            val pm = context.packageManager
            val certs: Array<android.content.pm.Signature> = if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners ?: return false
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES).signatures
                    ?: return false
            }

            val digest = MessageDigest.getInstance("SHA-256")
            certs.any { sig ->
                val hash = digest.digest(sig.toByteArray())
                val hex = hash.joinToString("") { "%02X".format(it) }
                hex == EXPECTED_SIG_SHA256
            }
        } catch (e: Exception) {
            false
        }
    }
}
