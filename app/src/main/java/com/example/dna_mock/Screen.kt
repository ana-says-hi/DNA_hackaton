package com.example.dna_mock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dna_mock.ui.screens.AccountScreen
import com.example.dna_mock.ui.screens.ChatbotScreen
import com.example.dna_mock.ui.screens.HomeScreen
import com.example.dna_mock.ui.screens.MoreScreen
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Account : Screen("account", "My account", Icons.Default.Person)
    object AIChatbot : Screen("ai_chatbot", "AI Chat", Icons.Default.Face)
    object Topup : Screen("topup", "Top up", Icons.Default.AddCircleOutline)
    object More : Screen("more", "More", Icons.Default.MoreHoriz)
}

val navItems = listOf(Screen.Home, Screen.Account, Screen.AIChatbot, Screen.Topup, Screen.More)

@Composable
fun MainScreen() {
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundColor,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                navItems.forEachIndexed { index, screen ->
                    val selected = index == selectedItem
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint = if (selected) BrandColor else BrandColor.copy(alpha = 0.65f)
                            )
                        },
                        label = { Text(screen.label, color = BrandColor) },
                        selected = selected,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BrandColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedItem) {
                0 -> HomeScreen(onTopUpClick = { selectedItem = 3 })
                1 -> AccountScreen()
                2 -> ChatbotScreen()
                3 -> TopupScreen()
                4 -> MoreScreen()
            }
        }
    }
}

/** Demo only; no payment processor is connected. */
@Composable
fun TopupScreen() {
    var amount by rememberSaveable { mutableIntStateOf(10) }
    var showDemoNotice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Top up a subscription", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("DNA Rajaton Prepaid · 044 965 2870", color = Color.DarkGray)
        Text("Choose an amount", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20).forEach { choice ->
                FilterChip(
                    selected = amount == choice,
                    onClick = { amount = choice },
                    label = { Text("€$choice") }
                )
            }
        }
        Button(
            onClick = { showDemoNotice = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandColor)
        ) {
            Text("Continue with €$amount")
        }
        Text(
            "Demo only: no payment will be processed or applied to your subscription.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
    }

    if (showDemoNotice) {
        AlertDialog(
            onDismissRequest = { showDemoNotice = false },
            title = { Text("Demo top-up") },
            text = { Text("A real top-up requires a payment integration. No charge was made.") },
            confirmButton = {
                TextButton(onClick = { showDemoNotice = false }) { Text("OK") }
            }
        )
    }
}
