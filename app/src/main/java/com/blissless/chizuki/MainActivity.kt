package com.blissless.chizuki

import android.content.Context
import android.content.SharedPreferences
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.chizuki.MainViewModel
import com.blissless.chizuki.ContentItem
import com.blissless.chizuki.ContentDetails
import com.blissless.chizuki.ExploreScreen
import com.blissless.chizuki.HomeScreen
import com.blissless.chizuki.ScheduleScreen
import com.blissless.chizuki.SearchScreen
import com.blissless.chizuki.DetailScreen
import com.blissless.chizuki.ChizukiBottomNav
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

object ChizukiTheme {
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        content()
    }
}

class MainActivity : ComponentActivity() {

    private var userDataPrefs: SharedPreferences? = null

    private fun getUserDataStorage(): SharedPreferences {
        if (userDataPrefs == null) {
            userDataPrefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        }
        return userDataPrefs!!
    }

    internal fun getContinueWatchingList(): List<ContentItem> {
        val json = getUserDataStorage().getString("continue_watching", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveContinueWatchingList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("continue_watching", json).apply()
    }

    internal fun getPlanningToWatchList(): List<ContentItem> {
        val json = getUserDataStorage().getString("planning_to_watch", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun savePlanningToWatchList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("planning_to_watch", json).apply()
    }

    internal fun getCompletedList(): List<ContentItem> {
        val json = getUserDataStorage().getString("completed", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveCompletedList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("completed", json).apply()
    }

    fun getOnHoldList(): List<ContentItem> {
        val json = getUserDataStorage().getString("onhold", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOnHoldList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("onhold", json).apply()
    }

    fun getDroppedList(): List<ContentItem> {
        val json = getUserDataStorage().getString("dropped", null) ?: return emptyList()
        return try {
            parseStoredContentList(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDroppedList(items: List<ContentItem>) {
        val json = serializeContentList(items)
        getUserDataStorage().edit().putString("dropped", json).apply()
    }

    private fun serializeContentList(items: List<ContentItem>): String {
        return items.joinToString("|||") {
            "${it.id}|${it.name}|${it.type}|${it.posterUrl ?: ""}|${it.backdropUrl ?: ""}|${it.voteAverage}|${it.genreIds.joinToString(",")}|${it.progressPosition}|${it.progressDuration}|${it.progressSeason}|${it.progressEpisode}"
        }
    }

    private fun parseStoredContentList(json: String): List<ContentItem> {
        if (json.isEmpty()) return emptyList()
        return json.split("|||").mapNotNull { itemStr ->
            val parts = itemStr.split("|")
            if (parts.size >= 7) {
                ContentItem(
                    id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[1],
                    type = parts[2],
                    posterUrl = parts[3].ifEmpty { null },
                    backdropUrl = parts[4].ifEmpty { null },
                    voteAverage = parts[5].toDoubleOrNull() ?: 0.0,
                    genreIds = parts.getOrNull(6)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                    progressPosition = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                    progressDuration = parts.getOrNull(8)?.toLongOrNull() ?: 0L,
                    progressSeason = parts.getOrNull(9)?.toIntOrNull() ?: 1,
                    progressEpisode = parts.getOrNull(10)?.toIntOrNull() ?: 1
                )
            } else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            ChizukiApp(viewModel)
        }
    }
}

@Composable
fun ChizukiApp(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var listRefreshKey by remember { mutableIntStateOf(0) }
    var detailRefreshKey by remember { mutableIntStateOf(0) }
    var isPlayerActive by remember { mutableStateOf(false) }
    var showDetailScreen by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var searchFocus by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var streamResultJson by remember { mutableStateOf<String?>(null) }
    var currentServerIndex by remember { mutableIntStateOf(0) }
    var lastFailedUrl by remember { mutableStateOf<String?>(null) }
    var refetchCount by remember { mutableIntStateOf(0) }
    var currentContent by remember { mutableStateOf<ContentItem?>(null) }
    var currentSeason by remember { mutableIntStateOf(1) }
    var currentEpisode by remember { mutableIntStateOf(1) }
    var totalEpisodes by remember { mutableIntStateOf(0) }
    var savedPosition by remember { mutableLongStateOf(0L) }
    var contentDetails by remember { mutableStateOf<ContentDetails?>(null) }

    val context = LocalContext.current
    val activity = context as? MainActivity
    val focusManager = LocalFocusManager.current

    fun getContinueWatchingList(): List<ContentItem> {
        return activity?.getContinueWatchingList() ?: emptyList()
    }

    fun saveContinueWatchingList(items: List<ContentItem>) {
        activity?.saveContinueWatchingList(items)
    }

    fun getPlanningToWatchList(): List<ContentItem> {
        return activity?.getPlanningToWatchList() ?: emptyList()
    }

    fun savePlanningToWatchList(items: List<ContentItem>) {
        activity?.savePlanningToWatchList(items)
    }

    fun getCompletedList(): List<ContentItem> {
        return activity?.getCompletedList() ?: emptyList()
    }

    fun saveCompletedList(items: List<ContentItem>) {
        activity?.saveCompletedList(items)
    }

    fun getOnHoldList(): List<ContentItem> {
        return activity?.getOnHoldList() ?: emptyList()
    }

    fun saveOnHoldList(items: List<ContentItem>) {
        activity?.saveOnHoldList(items)
    }

    fun getDroppedList(): List<ContentItem> {
        return activity?.getDroppedList() ?: emptyList()
    }

    fun saveDroppedList(items: List<ContentItem>) {
        activity?.saveDroppedList(items)
    }

    fun onContentClick(item: ContentItem) {
        currentContent = item
        currentSeason = item.progressSeason
        currentEpisode = item.progressEpisode
        savedPosition = item.progressPosition
        totalEpisodes = 0
        showDetailScreen = true
        contentDetails = null
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            contentDetails = viewModel.getContentDetails(item)
        }
    }

    fun onPlayContent() {
        if (currentContent == null) {
            android.util.Log.e("Chizuki", "onPlayContent: currentContent is NULL, aborting")
            return
        }
        showDetailScreen = false
        currentVideoUrl = null
        streamResultJson = null
        currentServerIndex = 0
        isPlayerActive = true
        val item = currentContent!!
        android.util.Log.d("Chizuki", "===== onPlayContent START =====")
        android.util.Log.d("Chizuki", "onPlayContent: item='${item.name}' type=${item.type} id=${item.id}")
        android.util.Log.d("Chizuki", "onPlayContent: currentSeason=$currentSeason currentEpisode=$currentEpisode")
        android.util.Log.d("Chizuki", "onPlayContent: selectedExtensionAuthority=${viewModel.selectedExtensionAuthority.value}")
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("Chizuki", "onPlayContent: launching IO coroutine to fetch stream URL...")
            val fetchStart = System.currentTimeMillis()
            val result = viewModel.fetchStreamUrl(
                title = item.name,
                tmdbId = item.id,
                mediaType = item.type,
                season = if (item.type == "tv") currentSeason else null,
                episode = if (item.type == "tv") currentEpisode else null
            )
            val fetchTime = System.currentTimeMillis() - fetchStart
            android.util.Log.d("Chizuki", "onPlayContent: fetchStreamUrl completed in ${fetchTime}ms, result=$result")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (result == null || result.primaryUrl == null) {
                    android.util.Log.e("Chizuki", "onPlayContent: FAILED — result is null or no primaryUrl")
                    android.util.Log.e("Chizuki", "onPlayContent: result=$result")
                    isPlayerActive = false
                    Toast.makeText(
                        context,
                        "No stream available. Select an extension in Settings.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    android.util.Log.d("Chizuki", "onPlayContent: Got URL from extension: ${result.primaryUrl}")
                    android.util.Log.d("Chizuki", "onPlayContent: rawJson length=${result.rawJson.length}")
                    android.util.Log.d("Chizuki", "onPlayContent: rawJson=$result.rawJson")
                    streamResultJson = result.rawJson
                    // Pick the first playable server's URL (not Auto[0]) so
                    // currentServerIndex and currentVideoUrl are in sync —
                    // otherwise onPlaybackError's "try next server" logic
                    // would start from the wrong index.
                    val first = selectFirstServerUrl(result.rawJson)
                    if (first != null) {
                        android.util.Log.d("Chizuki", "onPlayContent: selectFirstServerUrl returned index=${first.first} url=${first.second}")
                        currentServerIndex = first.first
                        currentVideoUrl = first.second
                    } else {
                        android.util.Log.d("Chizuki", "onPlayContent: selectFirstServerUrl returned null, using primaryUrl")
                        currentVideoUrl = result.primaryUrl
                    }
                    android.util.Log.d("Chizuki", "onPlayContent: final currentVideoUrl=$currentVideoUrl")
                    android.util.Log.d("Chizuki", "===== onPlayContent END =====")
                }
            }
        }
    }

    fun fallbackRefetch() {
        val item = currentContent
        if (item == null) {
            android.util.Log.e("Chizuki", "fallbackRefetch: currentContent is NULL, aborting")
            return
        }
        android.util.Log.d("Chizuki", "===== fallbackRefetch START =====")
        android.util.Log.d("Chizuki", "fallbackRefetch: re-fetching for '${item.name}' (${item.type}) season=$currentSeason ep=$currentEpisode")
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fetchStart = System.currentTimeMillis()
            val result = viewModel.fetchStreamUrl(
                title = item.name,
                tmdbId = item.id,
                mediaType = item.type,
                season = if (item.type == "tv") currentSeason else null,
                episode = if (item.type == "tv") currentEpisode else null
            )
            val fetchTime = System.currentTimeMillis() - fetchStart
            android.util.Log.d("Chizuki", "fallbackRefetch: completed in ${fetchTime}ms, result=$result")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (result != null && result.primaryUrl != null) {
                    android.util.Log.d("Chizuki", "fallbackRefetch: got new URL: ${result.primaryUrl}")
                    streamResultJson = result.rawJson
                    // Pick the first playable server's URL (not Auto[0])
                    // so currentServerIndex and currentVideoUrl stay in sync.
                    val first = selectFirstServerUrl(result.rawJson)
                    if (first != null) {
                        currentServerIndex = first.first
                        currentVideoUrl = first.second
                        android.util.Log.d("Chizuki", "fallbackRefetch: using server ${first.second} at index ${first.first}")
                    } else {
                        currentVideoUrl = result.primaryUrl
                        android.util.Log.d("Chizuki", "fallbackRefetch: no servers object, using primaryUrl")
                    }
                    android.util.Log.d("Chizuki", "fallbackRefetch: final currentVideoUrl=$currentVideoUrl")
                } else {
                    android.util.Log.e("Chizuki", "fallbackRefetch: FAILED — result=$result")
                    Toast.makeText(context, "No stream available.", Toast.LENGTH_SHORT).show()
                }
                android.util.Log.d("Chizuki", "===== fallbackRefetch END =====")
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            isPlayerActive -> {
                isPlayerActive = false
                currentVideoUrl = null
                streamResultJson = null
            }
            showSearchScreen -> {
                showSearchScreen = false
                searchFocus = false
                viewModel.clearSearchResults()
            }
            showDetailScreen -> {
                showDetailScreen = false
                contentDetails = null
            }
            else -> {
                // On any main tab (Schedule, Explore, Home), close the app
                activity?.finish()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF080E1A), Color(0xFF040810))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ScheduleScreen(onContentClick = { item: ContentItem ->
                        onContentClick(item)
                    })
                    1 -> ExploreScreen(
                        viewModel = viewModel, 
                        onContentClick = { item: ContentItem ->
                            onContentClick(item)
                        },
                        onSearchClick = {
                            showSearchScreen = true
                            searchFocus = true
                        }
                    )
                    2 -> HomeScreen(
                        getContinueWatchingList = { getContinueWatchingList() },
                        getPlanningToWatchList = { getPlanningToWatchList() },
                        getCompletedList = { getCompletedList() },
                        getOnHoldList = { getOnHoldList() },
                        getDroppedList = { getDroppedList() },
                        refreshKey = listRefreshKey,
                        onContentClick = { item: ContentItem ->
                            onContentClick(item)
                        },
                        onSearchClick = {
                            showSearchScreen = true
                            searchFocus = true
                        }
                    )
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        ChizukiBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { tab: Int ->
                selectedTab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isPlayerActive && currentContent != null) {
            val isSeries = currentContent?.type == "tv"
            val seasons = contentDetails?.seasons ?: emptyList()
            val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1
            val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
            val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: totalEpisodes.coerceAtLeast(1)

            PlayerScreen(
                videoUrl = currentVideoUrl ?: "",
                streamResultJson = streamResultJson,
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                totalEpisodes = episodesInCurrentSeason,
                maxSeason = maxSeason,
                animeName = currentContent?.name ?: "",
                episodeLength = 1440,
                isLoadingStream = currentVideoUrl == null,
                currentCategory = "sub",
                forwardSkipSeconds = 15,
                backwardSkipSeconds = 5,
                autoPlayNextEpisode = true,
                savedPosition = savedPosition,
                isSeries = isSeries,
                onSavePosition = { position, dur ->
                    currentContent?.let { item ->
                        val continueList = getContinueWatchingList().toMutableList()
                        val index = continueList.indexOfFirst { it.id == item.id && it.type == item.type }
                        val existingItem = continueList.getOrNull(index)
                        val existingDuration = existingItem?.progressDuration ?: 0L
                        val actualDuration = if (dur > 0) dur else existingDuration

                        if (index != -1) {
                            // Update existing item
                            if (position > 0) {
                                continueList[index] = continueList[index].copy(
                                    progressPosition = position,
                                    progressDuration = actualDuration,
                                    progressSeason = currentSeason,
                                    progressEpisode = currentEpisode
                                )
                            }
                        } else if (position > 0) {
                            // Immediately add to Continue Watching on first position save
                            continueList.add(0, item.copy(
                                progressPosition = position,
                                progressDuration = actualDuration,
                                progressSeason = currentSeason,
                                progressEpisode = currentEpisode
                            ))
                        }
                        saveContinueWatchingList(continueList)
                        listRefreshKey++
                    }
                },
                onProgressUpdate = { },
                onPreviousEpisode = {
                    val seasons = contentDetails?.seasons ?: emptyList()
                    val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                    val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: 24
                    val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1

                    val oldSeason = currentSeason
                    val oldEpisode = currentEpisode
                    if (currentEpisode > 1) {
                        currentEpisode--
                    } else if (currentSeason > 1) {
                        currentSeason--
                        val prevSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                        currentEpisode = prevSeasonInfo?.episodeCount ?: 24
                    } else if (currentSeason == 1 && currentEpisode == 1) {
                        currentEpisode = 1
                    }
                    android.util.Log.d("Chizuki", "onPreviousEpisode: S${oldSeason}E${oldEpisode} -> S${currentSeason}E${currentEpisode}")
                    currentVideoUrl = null
                    val item = currentContent ?: return@PlayerScreen
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val url = viewModel.fetchStreamUrl(
                            title = item.name,
                            tmdbId = item.id,
                            mediaType = item.type,
                            season = if (item.type == "tv") currentSeason else null,
                            episode = if (item.type == "tv") currentEpisode else null
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.util.Log.d("Chizuki", "onPreviousEpisode: fetch result=${url?.primaryUrl}")
                            currentVideoUrl = url?.primaryUrl
                            if (url == null) {
                                Toast.makeText(context, "No stream available.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onNextEpisode = {
                    val seasons = contentDetails?.seasons ?: emptyList()
                    val currentSeasonInfo = seasons.find { it.seasonNumber == currentSeason }
                    val episodesInCurrentSeason = currentSeasonInfo?.episodeCount ?: 24
                    val maxSeason = seasons.maxOfOrNull { it.seasonNumber } ?: 1

                    val oldSeason = currentSeason
                    val oldEpisode = currentEpisode
                    if (currentEpisode < episodesInCurrentSeason) {
                        currentEpisode++
                    } else if (currentSeason < maxSeason) {
                        currentSeason++
                        currentEpisode = 1
                    }
                    android.util.Log.d("Chizuki", "onNextEpisode: S${oldSeason}E${oldEpisode} -> S${currentSeason}E${currentEpisode}")
                    currentVideoUrl = null
                    val item = currentContent ?: return@PlayerScreen
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val url = viewModel.fetchStreamUrl(
                            title = item.name,
                            tmdbId = item.id,
                            mediaType = item.type,
                            season = if (item.type == "tv") currentSeason else null,
                            episode = if (item.type == "tv") currentEpisode else null
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.util.Log.d("Chizuki", "onNextEpisode: fetch result=${url?.primaryUrl}")
                            currentVideoUrl = url?.primaryUrl
                            if (url == null) {
                                Toast.makeText(context, "No stream available.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onPlaybackError = {
                    val json = streamResultJson
                    val failedUrl = currentVideoUrl
                    android.util.Log.e("Chizuki", "===== onPlaybackError FIRED =====")
                    android.util.Log.e("Chizuki", "onPlaybackError: currentServerIndex=$currentServerIndex currentVideoUrl=$currentVideoUrl")
                    android.util.Log.e("Chizuki", "onPlaybackError: lastFailedUrl=$lastFailedUrl refetchCount=$refetchCount")
                    android.util.Log.e("Chizuki", "onPlaybackError: streamResultJson isNull=${json == null} len=${json?.length}")
                    // Guard: if this exact URL already failed, don't retry infinitely
                    if (failedUrl != null && failedUrl == lastFailedUrl && refetchCount >= 2) {
                        android.util.Log.e("Chizuki", "onPlaybackError: SAME URL already failed $refetchCount times, giving up")
                        lastFailedUrl = null
                        refetchCount = 0
                        false  // show error UI
                    } else if (json != null) {
                        android.util.Log.e("Chizuki", "onPlaybackError: full streamResultJson = $json")
                        val next = selectNextServerUrl(json, currentServerIndex)
                        android.util.Log.e("Chizuki", "onPlaybackError: selectNextServerUrl after idx=$currentServerIndex -> $next")
                        if (next != null && next.first != failedUrl) {
                            currentServerIndex++
                            currentVideoUrl = next.first
                            lastFailedUrl = failedUrl
                            android.util.Log.d("Chizuki", "onPlaybackError: trying next server ${next.second} (#${currentServerIndex}): ${next.first}")
                            Toast.makeText(
                                context,
                                "Switching to ${next.second} server…",
                                Toast.LENGTH_SHORT
                            ).show()
                            true   // signal PlayerScreen to auto-retry
                        } else {
                            android.util.Log.e("Chizuki", "onPlaybackError: no more servers to try, re-fetching from extension")
                            Toast.makeText(
                                context,
                                "All servers failed. Re-fetching…",
                                Toast.LENGTH_SHORT
                            ).show()
                            lastFailedUrl = failedUrl
                            refetchCount++
                            // Clear the URL so the player goes IDLE instead of
                            // retrying the same failed URL. fallbackRefetch()
                            // will set a new currentVideoUrl when the network
                            // call completes — LaunchedEffect(videoUrl) will
                            // then fire and prepare the player.
                            currentVideoUrl = null
                            currentServerIndex = 0
                            fallbackRefetch()
                            true   // we handled it (URL will update async)
                        }
                    } else {
                        android.util.Log.e("Chizuki", "onPlaybackError: streamResultJson is NULL, falling back to re-fetch")
                        lastFailedUrl = failedUrl
                        refetchCount++
                        currentVideoUrl = null
                        fallbackRefetch()
                        true
                    }
                },
                onServerChange = { serverUrl ->
                    android.util.Log.d("Chizuki", "===== onServerChange =====")
                    android.util.Log.d("Chizuki", "onServerChange: switching to $serverUrl")
                    // Sync currentServerIndex to the picked URL so future
                    // onPlaybackError calls start from the right place.
                    val json = streamResultJson
                    android.util.Log.d("Chizuki", "onServerChange: streamResultJson isNull=${json == null}")
                    if (json != null) {
                        val keys = listOf("Hydrogen", "Titanium", "Oxygen", "Lithium", "Helium")
                        try {
                            val servers = org.json.JSONObject(json).optJSONObject("servers")
                            if (servers != null) {
                                android.util.Log.d("Chizuki", "onServerChange: servers keys=${servers.keys().asSequence().toList()}")
                                keys.forEachIndexed { idx, key ->
                                    val srv = servers.optJSONObject(key) ?: return@forEachIndexed
                                    val u = srv.optString("m3u8", "")
                                        .ifBlank { srv.optString("mp4", "") }
                                        .ifBlank { srv.optString("dash", "") }
                                    android.util.Log.d("Chizuki", "onServerChange:   [$idx] $key url=${u.take(120)}")
                                    if (u == serverUrl) {
                                        currentServerIndex = idx
                                        android.util.Log.d("Chizuki", "onServerChange: MATCH — synced currentServerIndex=$idx for $key")
                                    }
                                }
                            } else {
                                android.util.Log.w("Chizuki", "onServerChange: NO 'servers' object in JSON")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Chizuki", "onServerChange: failed to sync index: ${e.message}", e)
                        }
                    }
                    android.util.Log.d("Chizuki", "onServerChange: setting currentVideoUrl=$serverUrl")
                    currentVideoUrl = serverUrl
                },
                onClose = {
                    isPlayerActive = false
                    currentVideoUrl = null
                    streamResultJson = null
                }
            )
        }

        if (showSearchScreen) {
            SearchScreen(
                viewModel = viewModel,
                onContentClick = { item ->
                    showSearchScreen = false
                    searchFocus = false
                    onContentClick(item)
                },
                shouldFocus = searchFocus
            )
        }

        if (showDetailScreen && currentContent != null) {
            DetailScreen(
                content = currentContent!!,
                details = contentDetails,
                onPlayClick = { season: Int, episode: Int ->
                    currentSeason = season
                    currentEpisode = episode
                    onPlayContent()
                },
                onBackClick = {
                    showDetailScreen = false
                    contentDetails = null
                },
                onAddToList = { listType ->
                    val item = currentContent!!
                    // Remove from all lists first (only one list entry at a time)
                    var continueList = getContinueWatchingList().toMutableList()
                    var planningList = getPlanningToWatchList().toMutableList()
                    var completedList = getCompletedList().toMutableList()
                    var onHoldList = getOnHoldList().toMutableList()
                    var droppedList = getDroppedList().toMutableList()
                    
                    continueList.removeAll { it.id == item.id && it.type == item.type }
                    planningList.removeAll { it.id == item.id && it.type == item.type }
                    completedList.removeAll { it.id == item.id && it.type == item.type }
                    onHoldList.removeAll { it.id == item.id && it.type == item.type }
                    droppedList.removeAll { it.id == item.id && it.type == item.type }
                    
                    when (listType) {
                        "continue" -> {
                            continueList.add(0, item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "planning" -> {
                            planningList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "completed" -> {
                            completedList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "onhold" -> {
                            onHoldList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                        "dropped" -> {
                            droppedList.add(item)
                            saveContinueWatchingList(continueList)
                            savePlanningToWatchList(planningList)
                            saveCompletedList(completedList)
                            saveOnHoldList(onHoldList)
                            saveDroppedList(droppedList)
                        }
                    }
                    listRefreshKey++
                    detailRefreshKey++
                },
                onRemoveFromList = { listType ->
                    val item = currentContent!!
                    when (listType) {
                        "continue" -> {
                            val list = getContinueWatchingList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveContinueWatchingList(list)
                        }
                        "planning" -> {
                            val list = getPlanningToWatchList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            savePlanningToWatchList(list)
                        }
                        "completed" -> {
                            val list = getCompletedList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveCompletedList(list)
                        }
                        "onhold" -> {
                            val list = getOnHoldList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveOnHoldList(list)
                        }
                        "dropped" -> {
                            val list = getDroppedList().toMutableList()
                            list.removeAll { it.id == item.id && it.type == item.type }
                            saveDroppedList(list)
                        }
                    }
                    listRefreshKey++
                    detailRefreshKey++
                },
                getContinueWatchingList = { getContinueWatchingList() },
                getPlanningToWatchList = { getPlanningToWatchList() },
                getCompletedList = { getCompletedList() },
                getOnHoldList = { getOnHoldList() },
                getDroppedList = { getDroppedList() },
                refreshKey = detailRefreshKey,
                onContentClick = { item -> onContentClick(item) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Server-selection helpers — top-level so they can be forward-referenced
// from anywhere in ChizukiApp without Kotlin's local-function ordering
// restriction. These are pure functions over the extension's JSON response.
// ---------------------------------------------------------------------------

private val VIDKING_SERVER_KEYS = listOf(
    "Hydrogen", "Titanium", "Oxygen", "Lithium", "Helium"
)

/**
 * Returns (index, url) of the first playable server in the JSON, or null
 * if no servers have a stream URL. Used to pick the initial playback URL
 * — keeps currentServerIndex in sync with currentVideoUrl so that
 * onPlaybackError's "try next server" logic starts from the right place.
 */
fun selectFirstServerUrl(json: String): Pair<Int, String>? {
    android.util.Log.d("Chizuki", "selectFirstServerUrl: parsing JSON (${json.length} chars)")
    return try {
        val obj = org.json.JSONObject(json)
        val servers = obj.optJSONObject("servers")
        if (servers == null) {
            android.util.Log.w("Chizuki", "selectFirstServerUrl: NO 'servers' object in JSON")
            android.util.Log.w("Chizuki", "selectFirstServerUrl: JSON keys=${obj.keys().asSequence().toList()}")
            return null
        }
        android.util.Log.d("Chizuki", "selectFirstServerUrl: servers keys=${servers.keys().asSequence().toList()}")
        VIDKING_SERVER_KEYS.forEachIndexed { idx, key ->
            val srv = servers.optJSONObject(key)
            if (srv == null) {
                android.util.Log.d("Chizuki", "selectFirstServerUrl:   [$idx] $key = null (not present)")
                return@forEachIndexed
            }
            val url = srv.optString("m3u8", "")
                .ifBlank { srv.optString("mp4", "") }
                .ifBlank { srv.optString("dash", "") }
            val error = srv.optString("error", "")
            android.util.Log.d("Chizuki", "selectFirstServerUrl:   [$idx] $key: url=${url.take(150)} error=$error")
            if (url.isNotBlank()) return idx to url
        }
        android.util.Log.w("Chizuki", "selectFirstServerUrl: no servers have a stream URL")
        null
    } catch (e: Exception) {
        android.util.Log.e("Chizuki", "selectFirstServerUrl error: ${e.message}", e)
        null
    }
}

/**
 * Pick the next playable server URL after [currentIdx].
 *
 * Skips servers whose `m3u8`/`mp4`/`dash` are all empty (e.g. Titanium
 * when its upstream is broken, or Helium whose route 404s). Returns the
 * next server's (url, name), or null if no more servers.
 */
fun selectNextServerUrl(json: String, currentIdx: Int): Pair<String, String>? {
    android.util.Log.d("Chizuki", "selectNextServerUrl: currentIdx=$currentIdx")
    return try {
        val obj = org.json.JSONObject(json)
        val servers = obj.optJSONObject("servers")
        if (servers == null) {
            android.util.Log.w("Chizuki", "selectNextServerUrl: NO 'servers' object")
            return null
        }
        android.util.Log.d("Chizuki", "selectNextServerUrl: servers keys=${servers.keys().asSequence().toList()}")
        val filtered = VIDKING_SERVER_KEYS.mapNotNull { key ->
            val srv = servers.optJSONObject(key) ?: return@mapNotNull null
            val url = srv.optString("m3u8", "")
                .ifBlank { srv.optString("mp4", "") }
                .ifBlank { srv.optString("dash", "") }
                .ifBlank { null }
            if (url != null) {
                android.util.Log.d("Chizuki", "selectNextServerUrl:   $key -> url=${url.take(150)}")
                url to key
            } else {
                android.util.Log.d("Chizuki", "selectNextServerUrl:   $key -> no URL (skipping)")
                null
            }
        }
        android.util.Log.d("Chizuki", "selectNextServerUrl: filtered ${filtered.size} servers with URLs")
        val next = filtered.getOrNull(currentIdx + 1)
        android.util.Log.d("Chizuki", "selectNextServerUrl: next after idx=$currentIdx -> $next")
        next
    } catch (e: Exception) {
        android.util.Log.e("Chizuki", "selectNextServerUrl error: ${e.message}", e)
        null
    }
}
