package com.example.dna_mock.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dna_mock.notifications.NotificationInbox
import com.example.dna_mock.notifications.NotificationPreview
import com.example.dna_mock.notifications.NotificationReaderService
import com.example.dna_mock.notifications.isNotificationReaderEnabled
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor
import java.text.DateFormat
import java.util.Date

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAccess by remember {
        mutableStateOf(runCatching { isNotificationReaderEnabled(context) }.getOrDefault(false))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = runCatching { isNotificationReaderEnabled(context) }.getOrDefault(false)
                if (hasAccess) NotificationReaderService.refreshIfConnected()
                else NotificationInbox.replace(emptyList())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AccountScreenContent(
        hasAccess = hasAccess,
        notifications = NotificationInbox.items,
        onRequestPermission = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        onManagePermission = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        onRefresh = { NotificationReaderService.refreshIfConnected() }
    )
}

/**
 * Stateless rendering entrypoint: previews never query Android notification settings.
 * The live screen passes the current permission state and active notifications.
 */
@Composable
private fun AccountScreenContent(
    hasAccess: Boolean,
    notifications: List<NotificationPreview>,
    onRequestPermission: () -> Unit,
    onManagePermission: () -> Unit,
    onRefresh: () -> Unit
) {
    var openedNotification by remember { mutableStateOf<NotificationPreview?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("DNA Rajaton Prepaid", color = Color(0xFFC81C74), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("044 965 2870", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Demo account details", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = BrandColor)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Notification reader",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Read currently active notifications from this phone, including suspicious texts " +
                        "and app alerts. Android notification access is required."
                )
                Text(
                    "Privacy: granting notification access can expose sensitive messages from other " +
                        "apps. This demo shows previews locally in memory and does not upload them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                if (!hasAccess) {
                    Button(
                        onClick = {
                            onRequestPermission()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandColor)
                    ) {
                        Text("Enable notification access")
                    }
                    Text(
                        "Enable the DNA mock app in Android Settings, then return here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notification access enabled",
                            modifier = Modifier.weight(1f),
                            color = BrandColor
                        )
                        IconButton(onClick = { onRefresh() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh notifications")
                        }
                    }
                    TextButton(onClick = {
                        onManagePermission()
                    }) {
                        Text("Manage or revoke notification access")
                    }
                    if (notifications.isEmpty()) {
                        Text("No readable active notifications.", color = Color.Gray)
                    } else {
                        Text(
                            "Active notifications (" + notifications.size + ")",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        notifications.forEach { item ->
                            NotificationPreviewRow(item) { openedNotification = item }
                        }
                    }
                }
            }
        }
    }

    openedNotification?.let { item ->
        AlertDialog(
            onDismissRequest = { openedNotification = null },
            title = { Text(item.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.appName, color = BrandColor, fontWeight = FontWeight.Medium)
                    Text(item.content.ifBlank { "No message text was provided." })
                }
            },
            confirmButton = {
                TextButton(onClick = { openedNotification = null }) { Text("Close") }
            }
        )
    }
}


@Composable
private fun NotificationPreviewRow(item: NotificationPreview, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(0xFFFCF9FA),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E1E5))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.appName,
                    color = BrandColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.postedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.content.isNotBlank()) {
                Text(item.content, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
            }
        }
    }
}

// This preview displays the actual UI without requesting notification-listener access.
@Preview(name = "Account - permission needed", showBackground = true)
@Composable
private fun AccountPermissionPreview() {
    com.example.dna_mock.ui.theme.DNA_mockTheme {
        AccountScreenContent(
            hasAccess = false,
            notifications = emptyList(),
            onRequestPermission = {},
            onManagePermission = {},
            onRefresh = {}
        )
    }
}

@Preview(name = "Account - notifications enabled", showBackground = true)
@Composable
private fun AccountNotificationsPreview() {
    com.example.dna_mock.ui.theme.DNA_mockTheme {
        AccountScreenContent(
            hasAccess = true,
            notifications = listOf(
                NotificationPreview(
                    key = "preview-alert",
                    appName = "Messages",
                    title = "Security alert",
                    content = "A suspicious link was sent to your number. Review before opening.",
                    postedAt = 1783500000000L
                )
            ),
            onRequestPermission = {},
            onManagePermission = {},
            onRefresh = {}
        )
    }
}
