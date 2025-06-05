package com.example.myapplication.model

data class GameState(
    val lives: Int = 3,
    val isGameOver: Boolean = false,
    val crashToast: Boolean = false,
    val isGameRunning: Boolean = true,
    val carLane: Int = 2,
    val odometer: Int = 0,
    val coins: List<Pair<Int, Float>> = emptyList(),
    val collectedCoins: Int = 0
) 