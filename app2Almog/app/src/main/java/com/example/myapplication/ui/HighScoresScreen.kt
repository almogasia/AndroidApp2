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
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

@Composable
fun HighScoresScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Table", "Map")
    val context = LocalContext.current
    val repository = remember { HighScoresRepository(context) }
    val highScores by repository.getHighScores().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedScore by remember { mutableStateOf<HighScore?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
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
fun HighScoresTableFragment(highScores: List<HighScore>, selectedScore: HighScore?, onScoreClick: (HighScore) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Player", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Score", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Coins", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Mode", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
        items(highScores.sortedByDescending { it.score }) { score ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(if (score == selectedScore) Color.LightGray else Color.Transparent)
                    .clickable { onScoreClick(score) },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(score.playerName, modifier = Modifier.weight(1f))
                Text(score.score.toString(), modifier = Modifier.weight(1f))
                Text(score.coins.toString(), modifier = Modifier.weight(1f))
                Text(score.gameMode, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HighScoresMapFragment(highScores: List<HighScore>, selectedScore: HighScore?) {
    val context = LocalContext.current
    val defaultLocation = Pair(32.0853, 34.7818) // Tel Aviv
    val validScores = highScores.filter { it.latitude != null && it.longitude != null }
    val effectiveSelectedScore = selectedScore ?: validScores.lastOrNull()
    val selectedLocation = effectiveSelectedScore?.let {
        if (it.latitude != null && it.longitude != null)
            Pair(it.latitude, it.longitude)
        else null
    } ?: defaultLocation

    Box(modifier = Modifier.fillMaxSize()) {
        if (validScores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No high scores with location yet!")
            }
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        
                        // Create the HTML content with Leaflet
                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <title>High Scores Map</title>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                <style>
                                    html, body, #map {
                                        height: 100%;
                                        margin: 0;
                                        padding: 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="map"></div>
                                <script>
                                    var map = L.map('map').setView([${selectedLocation.first}, ${selectedLocation.second}], 14);
                                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        attribution: '© OpenStreetMap contributors'
                                    }).addTo(map);
                                    
                                    var markers = [];
                                    ${validScores.joinToString("\n") { score ->
                                        """
                                        var marker = L.marker([${score.latitude}, ${score.longitude}])
                                            .bindPopup('${score.playerName} (${score.score})<br>Coins: ${score.coins}')
                                            .addTo(map);
                                        ${if (score == effectiveSelectedScore) "marker.openPopup();" else ""}
                                        markers.push(marker);
                                        """
                                    }}
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
} 