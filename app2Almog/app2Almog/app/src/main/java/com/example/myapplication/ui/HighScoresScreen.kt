package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.myapplication.data.HighScoresRepository
import com.example.myapplication.model.HighScore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun HighScoresScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Table", "Map")
    val context = LocalContext.current
    val repository = remember { HighScoresRepository(context) }
    val highScores by repository.getHighScores().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedScore by remember { mutableStateOf<HighScore?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> HighScoresTableFragment(highScores, selectedScore) { score ->
                    selectedScore = score
                    selectedTab = 1 // Switch to map tab on click
                }
                1 -> HighScoresMapFragment(highScores, selectedScore)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    scope.launch {
                        repository.clearHighScores()
                    }
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Clear Scores")
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Back to Menu")
            }
        }
    }
}

@Composable
fun HighScoresTableFragment(
    highScores: List<HighScore>,
    selectedScore: HighScore?,
    onScoreClick: (HighScore) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "High Scores",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (highScores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No high scores yet!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text("Rank", modifier = Modifier.weight(0.8f).padding(end = 4.dp), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Name", modifier = Modifier.weight(1.5f).padding(start = 4.dp, end = 4.dp), fontSize = 14.sp)
                        Text("Score", modifier = Modifier.weight(1f).padding(horizontal = 4.dp), fontSize = 14.sp)
                        Text("Coins", modifier = Modifier.weight(0.8f).padding(horizontal = 4.dp), fontSize = 14.sp)
                        Text("Mode", modifier = Modifier.weight(1.2f).padding(start = 4.dp, end = 4.dp), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date", modifier = Modifier.weight(1.2f).padding(start = 4.dp), fontSize = 14.sp)
                    }
                    Divider()
                }

                itemsIndexed(highScores) { index, score ->
                    val isSelected = selectedScore == score
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onScoreClick(score) }
                    ) {
                        HighScoreRow(rank = index + 1, score = score)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun HighScoreRow(rank: Int, score: HighScore) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val (modeType, modeSpeed) = remember(score.gameMode) {
        when {
            score.gameMode.contains("button") -> "Button" to if (score.gameMode.contains("fast")) "Fast" else "Slow"
            score.gameMode.contains("sensor") -> "Sensor" to ""
            else -> score.gameMode to ""
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$rank",
            modifier = Modifier.weight(0.8f).padding(end = 4.dp),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = score.playerName,
            modifier = Modifier.weight(1.5f).padding(start = 4.dp, end = 4.dp)
        )
        Text(
            text = "${score.score}",
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        Text(
            text = "${score.coins}",
            modifier = Modifier.weight(0.8f).padding(horizontal = 4.dp)
        )
        Column(
            modifier = Modifier.weight(1.2f).padding(start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = modeType, fontSize = 14.sp)
            if (modeSpeed.isNotEmpty()) {
                Text(text = modeSpeed, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = dateFormat.format(Date(score.timestamp)),
            modifier = Modifier.weight(1.2f).padding(start = 4.dp),
            fontSize = 14.sp
        )
    }
}

@Composable
fun HighScoresMapFragment(highScores: List<HighScore>, selectedScore: HighScore?) {
    val context = LocalContext.current
    val mapHeight = 650.dp // or any height you prefer
    val defaultLocation = org.osmdroid.util.GeoPoint(32.0853, 34.7818) // Tel Aviv
    val validScores = highScores.filter { it.latitude != null && it.longitude != null }
    val effectiveSelectedScore = selectedScore ?: validScores.lastOrNull()
    val selectedGeoPoint = effectiveSelectedScore?.let {
        if (it.latitude != null && it.longitude != null)
            org.osmdroid.util.GeoPoint(it.latitude, it.longitude)
        else null
    } ?: defaultLocation

    // OSMDroid config (required)
    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (validScores.isEmpty()) {
            Text("No high scores with location yet!")
        } else {
            AndroidView(
                factory = { ctx ->
                    org.osmdroid.views.MapView(ctx).apply {
                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        controller.setCenter(selectedGeoPoint)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
                update = { mapView ->
                    mapView.overlays.clear()
                    validScores.forEach { score ->
                        if (score.latitude != null && score.longitude != null) {
                            val marker = org.osmdroid.views.overlay.Marker(mapView).apply {
                                position = org.osmdroid.util.GeoPoint(score.latitude, score.longitude)
                                title = "${score.playerName} (${score.score})"
                                snippet = "Coins: ${score.coins}"
                                setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(marker)
                        }
                    }
                    mapView.invalidate()
                }
            )
        }
    }
} 