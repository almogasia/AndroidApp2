package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.model.HighScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "high_scores")

class HighScoresRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun addHighScore(highScore: HighScore) {
        context.dataStore.edit { preferences ->
            val currentScores = getHighScores(preferences)
            val newScores = (currentScores + highScore)
                .sortedByDescending { it.score }
                .take(10) // Keep only top 10 scores
            
            preferences[highScoresKey] = json.encodeToString(newScores)
        }
    }

    suspend fun clearHighScores() {
        context.dataStore.edit { preferences ->
            preferences.remove(highScoresKey)
        }
    }

    fun getHighScores(): Flow<List<HighScore>> {
        return context.dataStore.data.map { preferences ->
            getHighScores(preferences)
        }
    }

    private fun getHighScores(preferences: Preferences): List<HighScore> {
        val scoresJson = preferences[highScoresKey] ?: return emptyList()
        return try {
            json.decodeFromString(scoresJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val highScoresKey = stringPreferencesKey("high_scores")
    }
} 