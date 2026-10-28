package com.floatscript.app.update

import android.content.Context
import android.os.Build

/**
 * Whether this build came from Google Play.
 *
 * Self-updating by downloading an APK is fine for the GitHub distribution but
 * violates Play policy, so the update UI is disabled when Play installed us.
 */
object InstallSource {

    private const val PLAY = "com.android.vending"

    fun isFromPlay(context: Context): Boolean = runCatching {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        installer == PLAY
    }.getOrDefault(false)
}
