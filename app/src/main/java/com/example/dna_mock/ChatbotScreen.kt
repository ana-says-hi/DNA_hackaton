package com.example.dna_mock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor
import com.example.dna_mock.ui.viewmodel.ChatMessage
import com.example.dna_mock.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(viewModel: ChatViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    // Raw JSON Inspector Dialog
    if (viewModel.isJsonDialogOpen) {
        Dialog(onDismissRequest = { viewModel.dismissJsonDialog() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Raw JSON Response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.dismissJsonDialog() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = viewModel.selectedRawJson ?: "No JSON data",
                                color = Color(0xFFD4D4D4),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = BackgroundColor) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandColor
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BrandColor.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text("Previous Chat: Security Check") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(viewModel.chatTitle, color = BackgroundColor) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History", tint = BackgroundColor)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BrandColor)
                )
            },
            containerColor = BackgroundColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(viewModel.messages.size) { index ->
                        val message = viewModel.messages[index]
                        ChatBubble(
                            message = message,
                            onViewJsonClicked = { json -> viewModel.showRawJson(json) }
                        )
                    }
                }

                // Input Area
                Surface(
                    color = BackgroundColor,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = viewModel.inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Describe security concern...") },
                            textStyle = TextStyle(color = Color.Black),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = BrandColor
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.sendMessage() }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = BrandColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onViewJsonClicked: (String) -> Unit) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) ProfilePicture(isAI = true)
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = if (isUser) BrandColor else Color(0xFFE8E1E5),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!isUser && message.steps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recommended Steps:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandColor)
                    message.steps.forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                }

                if (!isUser && message.questions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    message.questions.forEach { q ->
                        Text("❓ $q", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Color(0xFF5A2A68))
                    }
                }

                if (!isUser && message.rawJson != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "{ } View Raw JSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onViewJsonClicked(message.rawJson) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isUser) ProfilePicture(isAI = false)
    }
}

@Composable
fun ProfilePicture(isAI: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isAI) Color(0xFFD1C4E9) else BrandColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAI) Icons.Default.SmartToy else Icons.Default.Person,
            contentDescription = null,
            tint = if (isAI) BrandColor else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatbotScreenPreview() {
    ChatbotScreen()
}