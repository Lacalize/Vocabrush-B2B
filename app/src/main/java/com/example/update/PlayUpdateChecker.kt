package com.example.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play in-app updates. Replaces the old ForceUpdateManager, which sent users to an external
 * APK URL - Play's Device and Network Abuse policy forbids apps updating themselves outside Play.
 *
 * Forcing an update is controlled from the Play Console: give a release an in-app update priority
 * of 4 or 5 and installed copies below it get Play's full-screen (IMMEDIATE) update flow. Lower
 * priorities are left to Play's normal auto-update. Only works for installs that came from Play,
 * so on sideloaded debug APKs this is a silent no-op.
 *
 * Must be constructed before the activity is STARTED (i.e. in onCreate) because it registers an
 * activity-result launcher.
 */
class PlayUpdateChecker(private val activity: ComponentActivity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    private val updateLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            // User backed out of a required update - ask again (resumeIfInProgress re-checks on resume).
            Log.w(TAG, "Immediate update flow not completed: ${result.resultCode}")
        }
    }

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val required = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.updatePriority() >= FORCE_UPDATE_PRIORITY &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                if (required) {
                    appUpdateManager.startUpdateFlowForResult(
                        info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
            .addOnFailureListener { e -> Log.d(TAG, "In-app update check unavailable: ${e.message}") }
    }

    /** An IMMEDIATE update interrupted by leaving the app must be resumed when the user returns. */
    fun resumeIfInProgress() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                } else {
                    checkForUpdate()
                }
            }
            .addOnFailureListener { e -> Log.d(TAG, "In-app update check unavailable: ${e.message}") }
    }

    companion object {
        private const val TAG = "PlayUpdateChecker"
        private const val FORCE_UPDATE_PRIORITY = 4
    }
}
