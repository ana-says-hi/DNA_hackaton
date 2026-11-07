package com.example.dna_mock

import AccountScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dna_mock.ui.screens.* // Import your separate screen files
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Account : Screen("account", "Account", Icons.Default.Person)
    object AIChatbot : Screen("ai_chatbot", "AI Chat", Icons.Default.Face)
    object Topup : Screen("topup", "Topup", Icons.Default.Add)
    object More : Screen("more", "More", Icons.Default.Menu)
}

val navItems = listOf(
    Screen.Home,
    Screen.Account,
    Screen.AIChatbot,
    Screen.Topup,
    Screen.More
)

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                navItems.forEachIndexed { index, screen ->
                    val isSelected = selectedItem == index
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                tint = if (isSelected) BrandColor else BrandColor.copy(alpha = 0.6f)
                            )
                        },
                        label = { Text(text = screen.label, color = BrandColor) },
                        selected = isSelected,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BrandColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> HomeScreen()
                1 -> AccountScreen()
                2 -> ChatbotScreen()
                3 -> TopupScreen()
                4 -> MoreScreen()
            }
        }
    }
}

@Composable
fun TopupScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
        Box(contentAlignment = Alignment.Center) {
            Text("Topup Balance", color = BrandColor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun MoreScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
        Box(contentAlignment = Alignment.Center) {
            Text("More Options", color = BrandColor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}