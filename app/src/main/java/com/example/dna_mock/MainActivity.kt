package com.example.dna_mock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dna_mock.ui.theme.DNA_mockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DNA_mockTheme {
                // Call your MainHomeScreen here
                MainScreen()
            }
        }
    }
}
