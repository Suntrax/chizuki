package com.blissless.chizuki

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

data class ListStatus(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

val listStatuses = listOf(
    ListStatus("continue", "Watching", Icons.Default.PlayArrow, StatusWatching),
    ListStatus("planning", "Planning", Icons.Default.Bookmark, StatusPlanning),
    ListStatus("completed", "Completed", Icons.Default.Check, StatusCompleted),
    ListStatus("onhold", "On Hold", Icons.Default.Pause, StatusPaused),
    ListStatus("dropped", "Dropped", Icons.Default.Delete, StatusDropped)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    content: ContentItem,
    details: ContentDetails?,
    onPlayClick: (season: Int, episode: Int) -> Unit,
    onBackClick: () -> Unit,
    onAddToList: (String) -> Unit,
    onRemoveFromList: ((String) -> Unit)? = null,
    getContinueWatchingList: () -> List<ContentItem>,
    getPlanningToWatchList: () -> List<ContentItem>,
    getCompletedList: () -> List<ContentItem>,
    getOnHoldList: () -> List<ContentItem>,
    getDroppedList: () -> List<ContentItem>,
    refreshKey: Int = 0,
    onContentClick: ((ContentItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var showEpisodeSelector by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableIntStateOf(content.progressSeason.takeIf { it > 0 } ?: 1) }
    var selectedEpisode by remember { mutableIntStateOf(content.progressEpisode.takeIf { it > 0 } ?: 1) }

    LaunchedEffect(refreshKey) {}

    val isSeries = content.type == "tv"
    val totalSeasons = details?.seasons?.size ?: 1
    val episodesPerSeason = details?.seasons?.find { it.seasonNumber == selectedSeason }?.episodeCount ?: 12
    val trailer = details?.trailers?.firstOrNull()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val continueList = getContinueWatchingList()
    val planningList = getPlanningToWatchList()
    val completedList = getCompletedList()
    val onHoldList = getOnHoldList()
    val droppedList = getDroppedList()

    val isInContinue = continueList.any { it.id == content.id && it.type == content.type }
    val isInPlanning = planningList.any { it.id == content.id && it.type == content.type }
    val isInCompleted = completedList.any { it.id == content.id && it.type == content.type }
    val isInOnHold = onHoldList.any { it.id == content.id && it.type == content.type }
    val isInDropped = droppedList.any { it.id == content.id && it.type == content.type }

    val currentStatus = when {
        isInContinue -> listStatuses[0]
        isInPlanning -> listStatuses[1]
        isInCompleted -> listStatuses[2]
        isInOnHold -> listStatuses[3]
        isInDropped -> listStatuses[4]
        else -> null
    }

    if (showEpisodeSelector) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSelector = false },
            sheetState = sheetState,
            containerColor = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showEpisodeSelector = false }) {
                        Icon(Icons.Filled.Close, "Close", tint = SilverLight)
                    }
                }

                if (isSeries && totalSeasons > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..totalSeasons).toList()) { season ->
                            val isSel = season == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) BlueAccent else DarkCard)
                                    .clickable { selectedSeason = season }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Season $season",
                                    color = if (isSel) Color.White else SilverDark,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(episodesPerSeason.coerceAtLeast(1)) { index ->
                        val ep = index + 1
                        val isCurrent = ep == content.progressEpisode && selectedSeason == (content.progressSeason.takeIf { it > 0 } ?: 1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) BlueAccent.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    selectedEpisode = ep
                                    showEpisodeSelector = false
                                    onPlayClick(selectedSeason, selectedEpisode)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) BlueAccent else DarkCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$ep",
                                    color = if (isCurrent) Color.White else SilverDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Episode $ep",
                                color = if (isCurrent) BlueAccent else Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Playing",
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { imageView ->
                        Glide.with(context)
                            .load(content.backdropUrl ?: content.posterUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = content.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(currentStatus.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentStatus.label,
                                color = currentStatus.color,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                if (details != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(0.5.dp, GlassStroke)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (details.posterUrl != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        android.widget.ImageView(ctx).apply {
                                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                        }
                                    },
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    update = { iv ->
                                        Glide.with(context)
                                            .load(details.posterUrl)
                                            .transition(DrawableTransitionOptions.withCrossFade())
                                            .into(iv)
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = content.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = String.format("%.1f", details.voteAverage),
                                            color = Color(0xFFFFD700),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Rating", color = SilverDark, fontSize = 11.sp)
                                    }
                                    if (isSeries && details.numberOfSeasons > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${details.numberOfSeasons}",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Seasons", color = SilverDark, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${details.numberOfEpisodes}",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Episodes", color = SilverDark, fontSize = 11.sp)
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val hours = details.runtime / 60
                                            val mins = details.runtime % 60
                                            Text(
                                                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Runtime", color = SilverDark, fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (details.genres.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        details.genres.take(3).forEach { genre ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .border(BorderStroke(0.5.dp, GlassStroke), RoundedCornerShape(50))
                                                    .background(DarkSurface)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = genre,
                                                    color = SilverDark,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (details.status.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (details.status == "Released" || details.status == "Returning Series") StatusCompleted
                                                    else SilverDark
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = details.status,
                                            color = SilverDark,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (details.tagline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = details.tagline,
                            color = SilverDark,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }

                    if (details.overview.isNotEmpty() && details.overview != "No description available.") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = details.overview,
                            color = SilverLight,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(0.5.dp, GlassStroke)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (isSeries) showEpisodeSelector = true
                                else onPlayClick(1, 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSeries) "Play Episode" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (trailer != null) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${trailer.key}"))
                                            )
                                        } catch (_: Exception) {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.youtube.com/watch?v=${trailer.key}")
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GlassStroke)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Trailer",
                                        tint = SilverLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Trailer", color = SilverLight, fontSize = 13.sp)
                                }
                            }

                            if (isSeries) {
                                Button(
                                    onClick = { showEpisodeSelector = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = SilverLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Episodes", color = SilverLight, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                if (details != null && details.cast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Cast",
                        color = BlueAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(details.cast.take(15), key = { it.id }) { castMember ->
                            CastCard(castMember)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My List",
                    color = BlueAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listStatuses) { status ->
                        val isSelected = when (status.key) {
                            "continue" -> isInContinue
                            "planning" -> isInPlanning
                            "completed" -> isInCompleted
                            "onhold" -> isInOnHold
                            "dropped" -> isInDropped
                            else -> false
                        }

                        StatusButton(
                            status = status,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected && onRemoveFromList != null) {
                                    onRemoveFromList(status.key)
                                } else {
                                    onAddToList(status.key)
                                }
                            }
                        )
                    }
                }

                if (currentStatus != null && content.progressPosition > 0 && content.progressDuration > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val progress = (content.progressPosition.toFloat() / content.progressDuration.toFloat()).coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                color = SilverDark,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = currentStatus.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = currentStatus.color,
                            trackColor = ProgressTrackBg
                        )
                    }
                }

                if (details != null && details.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "More Like This",
                        color = BlueAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(details.recommendations, key = { it.id }) { rec ->
                            RecommendationCard(
                                item = rec,
                                onClick = { onContentClick?.invoke(rec) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CastCard(cast: CastMember) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            if (cast.profileUrl != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    update = { iv ->
                        Glide.with(iv.context).load(cast.profileUrl).circleCrop().into(iv)
                    }
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = SilverDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cast.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = cast.character,
            color = SilverDark,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecommendationCard(item: ContentItem, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(0.5.dp, GlassStroke)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(165.dp)
            ) {
                if (item.posterUrl != null) {
                    AndroidView(
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { iv ->
                            Glide.with(context)
                                .load(item.posterUrl)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(iv)
                        }
                    )
                } else {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SilverDark,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, DarkBackground.copy(alpha = 0.6f))
                            )
                        )
                )
            }

            Text(
                text = item.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                color = SilverLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.voteAverage > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", item.voteAverage),
                        color = SilverDark,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusButton(
    status: ListStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) status.color.copy(alpha = 0.2f) else DarkCard
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, status.color)
        } else {
            BorderStroke(0.5.dp, GlassStroke)
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) status.color else UnreadGray)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = status.label,
                color = if (isSelected) status.color else SilverDark,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
