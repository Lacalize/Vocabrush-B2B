package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UpdateInfo(
    val isUpdateRequired: Boolean = false,
    val latestVersionCode: Long = 0L,
    val currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
    val apkUrl: String = ""
)

class ForceUpdateManager(private val context: Context) {

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private var remoteConfig: FirebaseRemoteConfig? = null

    init {
        initRemoteConfig()
    }

    private fun initRemoteConfig() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val config = FirebaseRemoteConfig.getInstance()

            // 1. Minimum fetch interval set to 0 seconds for development / instant testing
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
            }
            config.setConfigSettingsAsync(configSettings)

            // Default config values
            val defaults = mapOf<String, Any>(
                KEY_LATEST_VERSION_CODE to BuildConfig.VERSION_CODE.toLong(),
                KEY_APK_URL to ""
            )
            config.setDefaultsAsync(defaults)

            this.remoteConfig = config

            // Real-time config update listener
            config.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Config updated keys: ${configUpdate.updatedKeys}")
                    if (configUpdate.updatedKeys.contains(KEY_LATEST_VERSION_CODE) ||
                        configUpdate.updatedKeys.contains(KEY_APK_URL)
                    ) {
                        config.activate().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                checkVersion(config)
                            }
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Config update listener error: ${error.message}", error)
                }
            })

            // Initial fetch and activate
            fetchAndActivate()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Remote Config", e)
        }
    }

    fun fetchAndActivate() {
        remoteConfig?.let { config ->
            config.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Remote Config fetch and activate succeeded")
                    } else {
                        Log.w(TAG, "Remote Config fetch and activate failed")
                    }
                    checkVersion(config)
                }
        }
    }

    private fun checkVersion(config: FirebaseRemoteConfig) {
        val latestVersionCode = config.getLong(KEY_LATEST_VERSION_CODE)
        val apkUrl = config.getString(KEY_APK_URL)
        val currentVersionCode = BuildConfig.VERSION_CODE.toLong()

        Log.d(TAG, "Current Version: $currentVersionCode, Latest Version: $latestVersionCode, APK URL: $apkUrl")

        val isUpdateRequired = latestVersionCode > currentVersionCode && apkUrl.isNotBlank()

        _updateInfo.value = UpdateInfo(
            isUpdateRequired = isUpdateRequired,
            latestVersionCode = latestVersionCode,
            currentVersionCode = currentVersionCode,
            apkUrl = apkUrl
        )
    }

    fun launchUpdateIntent(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser for update URL: $url", e)
        }
    }

    companion object {
        private const val TAG = "ForceUpdateManager"
        const val KEY_LATEST_VERSION_CODE = "android_latest_version_code"
        const val KEY_APK_URL = "android_apk_url"

        /**
         * Helper to trigger MaterialAlertDialogBuilder directly on an Activity if needed.
         */
        fun showMaterialUpdateDialog(activity: Activity, apkUrl: String) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("發現新版本")
                .setMessage("為了保障您的使用體驗與修復已知錯誤，請立即更新至最新版本。")
                .setCancelable(false)
                .setPositiveButton("立即更新") { _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch browser", e)
                    }
                }
                .show()
        }
    }
}

/**
 * Non-dismissible Compose Force Update Dialog.
 */
@Composable
fun ForceUpdateDialog(
    apkUrl: String,
    onUpdateClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Cannot be dismissed by clicking outside or pressing back button
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(text = "發現新版本")
        },
        text = {
            Text(text = "為了保障您的使用體驗與修復已知錯誤，請立即更新至最新版本。")
        },
        confirmButton = {
            Button(
                onClick = { onUpdateClick(apkUrl) }
            ) {
                Text(text = "立即更新")
            }
        }
    )
}
