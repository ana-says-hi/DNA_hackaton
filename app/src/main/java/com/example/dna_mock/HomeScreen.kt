package com.example.dna_mock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

@Composable
fun HomeScreen(onTopUpClick: () -> Unit = {}) {
    var showDayChargeInfo by rememberSaveable { mutableStateOf(false) }
    val accent = Color(0xFFC81C74)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF8F5873), Color(0xFF551C3D), Color(0xFFB9849C))
                    )
                )
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(9.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DNA Rajaton Prepaid", color = accent, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("044 965 2870", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171717))
                    Spacer(Modifier.height(4.dp))
                    Text("Valid until 13.06.2027", color = Color.DarkGray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(9.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                    Text("Balance remaining", fontSize = 14.sp)
                    Text("6,00 €", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Text("Will last for 6 days with normal day charge.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(15.dp))
                    OutlinedButton(
                        onClick = onTopUpClick,
                        modifier = Modifier.fillMaxWidth().height(43.dp),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColor)
                    ) {
                        Text("Top up a subscription", fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Color(0xFFEDE9EB))
                TextButton(
                    onClick = { showDayChargeInfo = !showDayChargeInfo },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF202020))
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "More information on day charges",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        if (showDayChargeInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDayChargeInfo) "Collapse" else "Expand",
                        tint = Color.Gray
                    )
                }
                if (showDayChargeInfo) {
                    Text(
                        "A day charge is deducted when the prepaid subscription is used. " +
                                "The balance and days remaining are illustrative demo values; " +
                                "check your DNA account for live rates.",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}