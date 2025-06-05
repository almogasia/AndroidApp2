package com.example.myapplication.logic

import com.example.myapplication.model.GameState
import com.example.myapplication.utilities.GameConstants
import kotlin.random.Random

class GameManager(val lanes: Int = 5, val mode: String = "button_slow") {
    private var gameState = GameState(carLane = lanes / 2)
    private var obstacles: MutableList<Pair<Int, Float>> = mutableListOf() // (lane, relativeRow)
    private val maxObstacles = 4
    private val random = Random(System.currentTimeMillis())
    private var sensorSpeedMultiplier: Float = 1.5f
    private val baseStep = 0.015f // Movement per tick (1.5% of screen)

    fun setSensorSpeed(multiplier: Float) {
        sensorSpeedMultiplier = multiplier
    }

    fun updateGameState(newState: GameState) {
        gameState = newState
    }

    fun getGameState(): GameState = gameState

    fun getObstacles(): List<Pair<Int, Float>> = obstacles.toList()

    fun moveObstacle() {
        val speed = when (mode) {
            "button_fast" -> baseStep * 1.7f
            "sensor" -> baseStep * 0.8f * sensorSpeedMultiplier
            else -> baseStep
        }
        // Move all obstacles down
        obstacles = obstacles.map { (lane, row) -> lane to (row + speed) }.toMutableList()
        // Remove obstacles that have moved past the grid
        obstacles.removeAll { it.second >= 1.0f }
        // Randomly spawn new obstacles if less than maxObstacles
        if (obstacles.size < maxObstacles && random.nextFloat() < 0.08f) {
            val availableLanes = (0 until lanes).filter { lane ->
                obstacles.none { it.first == lane && it.second < 0.15f }
            }
            if (availableLanes.isNotEmpty()) {
                val newLane = availableLanes.random(random)
                obstacles.add(newLane to 0f) // Start at top
            }
        }
        // Update odometer
        gameState = gameState.copy(
            odometer = gameState.odometer + (speed * 100).toInt()
        )
        moveCoins(speed)
        // Randomly spawn coins
        if (random.nextFloat() < 0.02f) {
            spawnCoin()
        }
    }

    fun moveCoins(speed: Float) {
        val newCoins = gameState.coins.map { (lane, row) ->
            lane to (row + speed)
        }.filter { it.second < 1.0f }
        gameState = gameState.copy(coins = newCoins)
    }

    fun spawnCoin() {
        val lane = (0 until lanes).random(random)
        // Only spawn if no coin is already near the top in this lane
        if (gameState.coins.none { it.first == lane && it.second < 0.15f }) {
            gameState = gameState.copy(coins = gameState.coins + (lane to 0f))
        }
    }

    fun collectCoin() {
        val carLane = gameState.carLane
        val carRow = 0.8f // Car is at 80% of the game area height
        val collected = gameState.coins.filter {
            it.first == carLane && (it.second >= carRow && it.second < carRow + 0.08f)
        }
        if (collected.isNotEmpty()) {
            gameState = gameState.copy(
                coins = gameState.coins - collected.toSet(),
                collectedCoins = gameState.collectedCoins + collected.size
            )
        }
    }

    fun checkCollision(): Boolean {
        val carLane = gameState.carLane
        val carRow = 0.8f
        return obstacles.any { (lane, row) ->
            lane == carLane && (row >= carRow && row < carRow + 0.08f)
        }
    }

    fun handleCollision() {
        if (checkCollision()) {
            gameState = gameState.copy(
                lives = gameState.lives - 1,
                crashToast = true
            )
            // Remove the obstacle that caused the collision
            val carLane = gameState.carLane
            val carRow = 0.8f
            obstacles.removeAll { (lane, row) ->
                lane == carLane && (row >= carRow && row < carRow + 0.08f)
            }
            if (gameState.lives <= 0) {
                gameState = gameState.copy(
                    isGameOver = true,
                    isGameRunning = false
                )
            }
        }
    }

    fun moveCarLeft() {
        if (gameState.carLane > 0) {
            gameState = gameState.copy(carLane = gameState.carLane - 1)
        }
    }

    fun moveCarRight() {
        if (gameState.carLane < lanes - 1) {
            gameState = gameState.copy(carLane = gameState.carLane + 1)
        }
    }
} 