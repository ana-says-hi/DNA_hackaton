package com.example.dna_mock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

@Composable
fun HomeScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
        Box(contentAlignment = Alignment.Center) {
            Text("Home Screen", color = BrandColor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}