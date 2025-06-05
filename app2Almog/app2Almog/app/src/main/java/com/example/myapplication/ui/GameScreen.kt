package com.example.myapplication.ui

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.logic.GameManager
import com.example.myapplication.model.GameState
import com.example.myapplication.utilities.GameConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.myapplication.data.HighScoresRepository
import com.example.myapplication.model.HighScore
import android.util.Log
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.first

fun getCurrentLocation(): Pair<Double, Double> {
    // Tel Aviv (fake location); replace with real location logic later
    return 32.0853 to 34.7818
}

@Composable
fun GameScreen(navController: NavHostController, mode: String) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    val gameManager = remember { GameManager(lanes = 5, mode = mode) }
    var gameState by remember { mutableStateOf(gameManager.getGameState()) }
    val obstacles = remember { mutableStateListOf<Pair<Int, Float>>() }
    val highScoresRepository = remember { HighScoresRepository(context) }
    var scoreSaved by remember { mutableStateOf(false) }

    // --- SENSOR MODE LOGIC ---
    if (mode == "sensor") {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastMoveTime by remember { mutableStateOf(0L) }
        val moveCooldown = 400L // ms between moves to avoid jitter (increased for easier single-tile moves)
        var sensorSpeed by remember { mutableStateOf(1.5f) } // Default multiplier for sensor mode
        DisposableEffect(Unit) {
            val handler = Handler(Looper.getMainLooper())
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0] // left/right tilt (steering wheel, emulator Y-rot)
                    val z = event.values[2] // forward/backward tilt (emulator X-rot)
                    val now = System.currentTimeMillis()
                    // Map z: 6.93 (fastest, 3.0x), 0 (vertical, 1.5x), -6.93 (slowest, 0.5x)
                    sensorSpeed = when {
                        z >= 6.93f -> 3.0f // fastest
                        z <= -6.93f -> 0.5f // slowest
                        z in 0f..6.93f -> {
                            // Interpolate between 0 (1.5x) and 6.93 (3.0x)
                            val t = z / 6.93f
                            1.5f + t * (3.0f - 1.5f)
                        }
                        z in -6.93f..0f -> {
                            // Interpolate between -6.93 (0.5x) and 0 (1.5x)
                            val t = (z + 6.93f) / 6.93f
                            0.5f + t * (1.5f - 0.5f)
                        }
                        else -> 1.5f // fallback
                    }.coerceIn(0.5f, 3.0f)
                    // Car movement: steering wheel style (X axis)
                    if (now - lastMoveTime > moveCooldown) {
                        if (x > 2.0) { // left tilt, less sensitive
                            gameManager.moveCarLeft()
                            gameState = gameManager.getGameState()
                            lastMoveTime = now
                        } else if (x < -2.0) { // right tilt, less sensitive
                            gameManager.moveCarRight()
                            gameState = gameManager.getGameState()
                            lastMoveTime = now
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
        // Override obstacle speed in game loop
        LaunchedEffect(sensorSpeed) {
            gameManager.setSensorSpeed(sensorSpeed)
        }
    }
    // --- END SENSOR MODE LOGIC ---

    // Game loop (same as before, but will be upgraded for coins, odometer, etc.)
    var gameJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            gameJob?.cancel()
            gameManager.updateGameState(gameState.copy(isGameRunning = false))
        }
    }

    LaunchedEffect(gameState.isGameRunning) {
        try {
            if (gameState.isGameRunning) {
                gameJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
                while (gameState.isGameRunning) {
                    try {
                        obstacles.clear()
                        obstacles.addAll(gameManager.getObstacles())
                        delay(GameConstants.GAME_LOOP_DELAY)
                        if (gameState.lives > 0) {
                            gameManager.moveObstacle()
                            gameManager.collectCoin()
                            gameState = gameManager.getGameState()
                            // TODO: Move coins, update odometer, handle sensor/button mode
                            if (gameManager.checkCollision()) {
                                gameManager.handleCollision()
                                gameState = gameManager.getGameState()
                                try {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(
                                        GameConstants.VIBRATION_DURATION,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    ))
                                } catch (e: Exception) {
                                    // Ignore vibration errors
                                }
                                delay(GameConstants.COLLISION_DELAY)
                            }
                            if (gameState.lives == 0) {
                                Toast.makeText(context, "Game Over", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(gameState.lives) {
        if (gameState.lives == 0) {
            Toast.makeText(context, "Game Over", Toast.LENGTH_LONG).show()
            if (!scoreSaved) {
                scoreSaved = true
                // List of fake locations: Tel Aviv, Haifa, Jerusalem, Eilat, Be'er Sheva
                val fakeLocations = listOf(
                    32.0853 to 34.7818, // Tel Aviv
                    32.7940 to 34.9896, // Haifa
                    31.7683 to 35.2137, // Jerusalem
                    29.5581 to 34.9482, // Eilat
                    31.2520 to 34.7915  // Be'er Sheva
                )
                // Use .first() to get the current list from the Flow
                val highScores = highScoresRepository.getHighScores().first()
                val locationIndex = highScores.size % fakeLocations.size
                val (lat, lng) = fakeLocations[locationIndex]
                highScoresRepository.addHighScore(
                    HighScore(
                        playerName = "Player",
                        score = gameState.odometer,
                        coins = gameState.collectedCoins,
                        gameMode = mode,
                        latitude = lat,
                        longitude = lng
                    )
                )
            }
            delay(1500)
            navController.popBackStack("menu", inclusive = false)
        }
    }

    if (gameState.crashToast) {
        Toast.makeText(context, "Crash!", Toast.LENGTH_SHORT).show()
        try {
            val mediaPlayer = MediaPlayer.create(context, com.example.myapplication.R.raw.crash)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
        } catch (e: Exception) {
            // Ignore sound errors
        }
        gameManager.updateGameState(gameState.copy(crashToast = false))
        gameState = gameManager.getGameState()
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Lane separators over the entire screen (striped/dashed, 5/6 per line)
        val laneCount = 5
        val dashCount = 6
        val dashHeight = 40.dp
        val dashColor = Color.Gray.copy(alpha = 0.5f)

        // Road lanes (5 lanes) with separation
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.Center)
        ) {
            repeat(laneCount) { lane ->
                val safeLane = lane.coerceIn(0, laneCount - 1)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Lane separator
                    if (lane < laneCount - 1) {
                        Column(
                            Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(dashCount) {
                                Box(
                                    Modifier
                                        .height(dashHeight)
                                        .fillMaxWidth()
                                        .background(dashColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Game elements layer (obstacles and coins)
        val gameAreaHeight = 600.dp
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.Center)
        ) {
            repeat(laneCount) { lane ->
                val safeLane = lane.coerceIn(0, laneCount - 1)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    obstacles.filter { it.first == safeLane }.forEach { obs ->
                        val pos = obs.second.coerceIn(0f, 1f)
                        val minWeight = 0.0001f
                        Column(Modifier.fillMaxHeight()) {
                            Spacer(modifier = Modifier.weight(maxOf(pos, minWeight)))
                            Image(
                                painter = painterResource(id = com.example.myapplication.R.drawable.rock),
                                contentDescription = "Rock",
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(180.dp)
                            )
                            Spacer(modifier = Modifier.weight(maxOf(1f - pos, minWeight)))
                        }
                    }
                    gameState.coins.filter { it.first == safeLane }.forEach { coin ->
                        val pos = coin.second.coerceIn(0f, 1f)
                        val minWeight = 0.0001f
                        Column(Modifier.fillMaxHeight()) {
                            Spacer(modifier = Modifier.weight(maxOf(pos, minWeight)))
                            Image(
                                painter = painterResource(id = com.example.myapplication.R.drawable.coin),
                                contentDescription = "Coin",
                                modifier = Modifier
                                    .size(40.dp)
                            )
                            Spacer(modifier = Modifier.weight(maxOf(1f - pos, minWeight)))
                        }
                    }
                }
            }
        }

        // Top bar: Lives, Distance, Coins
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, end = 16.dp, start = 16.dp)
                .align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Lives
            repeat(gameState.lives) {
                Image(
                    painter = painterResource(id = com.example.myapplication.R.drawable.hearts),
                    contentDescription = "Heart",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            // Distance
            Text(
                text = "${gameState.odometer} m",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.width(16.dp))
            // Coins
            Image(
                painter = painterResource(id = com.example.myapplication.R.drawable.coin),
                contentDescription = "Coin",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "${gameState.collectedCoins}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Car (5 lanes)
        Row(
            Modifier
                .fillMaxWidth()
                .height(275.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(5) { lane ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (lane == gameState.carLane) {
                        Image(
                            painter = painterResource(id = com.example.myapplication.R.drawable.car),
                            contentDescription = "Car",
                            modifier = Modifier
                                .fillMaxWidth(1.0f)
                                .fillMaxHeight(1.0f)
                        )
                    }
                }
            }
        }

        // Controls (only if not in sensor mode)
        if (mode.startsWith("button")) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        gameManager.moveCarLeft()
                        gameState = gameManager.getGameState()
                    },
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .size(width = 70.dp, height = 70.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("←", fontSize = 54.sp, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = {
                        gameManager.moveCarRight()
                        gameState = gameManager.getGameState()
                    },
                    modifier = Modifier
                        .padding(end = 32.dp)
                        .size(width = 70.dp, height = 70.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("→", fontSize = 54.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
} 