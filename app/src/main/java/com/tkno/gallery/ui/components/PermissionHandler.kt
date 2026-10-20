package com.tkno.gallery.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tkno.gallery.ui.onboarding.OnboardingScreen

@Composable
fun PermissionHandler(
    bypassPermission: Boolean = false,
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    var isOnboardingCompleted by remember {
        mutableStateOf(prefs.getBoolean("onboarding_completed", false))
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.values.all { it }
        hasPermission = granted
        if (granted) {
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            isOnboardingCompleted = true
        }
    }

    // Refresh permission status when user returns to app (e.g. from system settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = requiredPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if ((hasPermission && isOnboardingCompleted) || bypassPermission) {
        onPermissionsGranted()
    } else {
        OnboardingScreen(
            initialPage = if (isOnboardingCompleted && !hasPermission) 2 else 0,
            onFinished = {
                if (hasPermission) {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    isOnboardingCompleted = true
                } else {
                    launcher.launch(requiredPermissions)
                }
            },
            onNotNow = {
                if (hasPermission) {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    isOnboardingCompleted = true
                } else {
                    launcher.launch(requiredPermissions)
                }
            }
        )
    }
}
