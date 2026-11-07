package com.example.dna_mock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

private data class MoreOption(val title: String, val description: String, val icon: ImageVector)
private val magenta = Color(0xFFCF2279)

@Composable
fun MoreScreen() {
    var opened by remember { mutableStateOf<MoreOption?>(null) }
    var pauseDialog by remember { mutableStateOf(false) }

    val safeBrowsing = MoreOption(
        "Turvallinen nettiselaus",
        "Safe browsing can help protect against malicious websites. Activation requires a DNA account connection.",
        Icons.Default.Shield
    )
    val fiveG = MoreOption(
        "5G connection",
        "Check whether your device and subscription support 5G in your DNA account.",
        Icons.Default.SignalCellularAlt
    )
    val management = MoreOption(
        "Management and safety numbers",
        "Manage subscription and safety numbers in your DNA account. Changes are unavailable in this demo.",
        Icons.Default.Tune
    )
    val roaming = MoreOption(
        "Data usage abroad",
        "Check roaming usage and current rates for your plan and destination before travelling.",
        Icons.Default.WifiTethering
    )
    val transactions = MoreOption(
        "Transactions",
        "Top-up and charge history requires a connection to your DNA account.",
        Icons.Default.ReceiptLong
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(23.dp))
        MoreSection("Add-on services", true) {
            MoreOptionRow(safeBrowsing) { opened = safeBrowsing }
            MoreOptionRow(fiveG) { opened = fiveG }
        }
        MoreSection("Others", false) {
            MoreOptionRow(management) { opened = management }
            MoreOptionRow(roaming) { opened = roaming }
            MoreOptionRow(transactions) { opened = transactions }
        }
        Column(Modifier.fillMaxWidth().background(BackgroundColor).padding(16.dp)) {
            Text("Pause subscription", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            Text(
                "You can pause your subscription. While paused, you will not be charged the " +
                    "daily rate, and the subscription cannot be used to place calls, send text " +
                    "messages or use data.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 23.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "The subscription pause fee is €5.00.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C5759)
            )
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = { pauseDialog = true },
                border = BorderStroke(1.dp, BrandColor),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColor)
            ) {
                Text("Pause subscription", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    opened?.let { option ->
        AlertDialog(
            onDismissRequest = { opened = null },
            title = { Text(option.title) },
            text = { Text(option.description) },
            confirmButton = { TextButton(onClick = { opened = null }) { Text("Close") } }
        )
    }
    if (pauseDialog) {
        AlertDialog(
            onDismissRequest = { pauseDialog = false },
            title = { Text("Pause subscription") },
            text = {
                Text("The reference screen lists a €5.00 fee. This is only a demo: " +
                    "no fee is charged and your subscription is not changed.")
            },
            confirmButton = { TextButton(onClick = { pauseDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun MoreSection(title: String, shaded: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(if (shaded) BackgroundColor else Color.White)
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF151515))
        Spacer(Modifier.height(1.dp))
        content()
    }
}

@Composable
private fun MoreOptionRow(option: MoreOption, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE9E5E6))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(option.icon, contentDescription = null, tint = magenta, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(15.dp))
            Text(
                option.title,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF191919)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = "Open " + option.title, tint = magenta)
        }
    }
}
