package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color

@Composable
fun MenuScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)) // light gray background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = com.example.myapplication.R.mipmap.ic_launcher),
                contentDescription = "App Icon",
                modifier = Modifier.size(160.dp).padding(bottom = 24.dp)
            )
            Text("Choose Game Mode", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate("game/button_slow") }, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Button Mode - Slow")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("game/button_fast") }, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Button Mode - Fast")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("game/sensor") }, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Sensor Mode")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate("highscores") }, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("High Scores")
            }
        }
    }
} 