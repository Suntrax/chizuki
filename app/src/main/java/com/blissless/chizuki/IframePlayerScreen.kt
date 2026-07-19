package com.blissless.chizuki

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IframePlayerScreen(
    videoUrl: String,
    isSeries: Boolean = false,
    currentSeason: Int = 1,
    currentEpisode: Int = 1,
    totalEpisodes: Int = 0,
    maxSeason: Int = 1,
    animeName: String = "",
    savedPosition: Long = 0L,
    onNextEpisode: (() -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    onSavePosition: ((position: Long, duration: Long) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLoading by remember { mutableStateOf(true) }

    var elapsedMs by remember { mutableLongStateOf(savedPosition) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            elapsedMs += 10_000L
            onSavePosition?.invoke(elapsedMs, elapsedMs)
        }
    }

    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSavePosition?.invoke(elapsedMs, elapsedMs)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webChromeClient = WebChromeClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        settings.mediaPlaybackRequiresUserGesture = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    loadUrl(videoUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        if (isSeries && (onNextEpisode != null || onPreviousEpisode != null)) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onPreviousEpisode?.invoke() },
                    enabled = onPreviousEpisode != null && (currentSeason > 1 || currentEpisode > 1),
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Episode",
                        tint = if (onPreviousEpisode != null && (currentSeason > 1 || currentEpisode > 1))
                            Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { onNextEpisode?.invoke() },
                    enabled = onNextEpisode != null && (currentSeason < maxSeason || currentEpisode < totalEpisodes),
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = if (onNextEpisode != null && (currentSeason < maxSeason || currentEpisode < totalEpisodes))
                            Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
