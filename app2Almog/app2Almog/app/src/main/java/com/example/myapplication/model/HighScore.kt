package com.example.myapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class HighScore(
    val playerName: String,
    val score: Int,
    val coins: Int,
    val gameMode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
) 