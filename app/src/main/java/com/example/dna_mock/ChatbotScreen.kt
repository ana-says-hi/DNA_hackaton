package com.example.dna_mock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dna_mock.data.ChatMessage
import com.example.dna_mock.data.ChatService
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen() {
    val chatService = remember { ChatService() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf(ChatMessage("Hello! How can I help you today?", false))) }
    var inputText by remember { mutableStateOf("") }
    var chatTitle by remember { mutableStateOf("New chat") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BackgroundColor
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandColor
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BrandColor.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text("Previous Chat: Topup Help") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = BrandColor
                    )
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(chatTitle, color = BackgroundColor) }, // Inverted text color
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "History",
                                tint = BackgroundColor // Inverted icon color
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BrandColor // Inverted ribbon background
                    )
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
                    items(messages) { message ->
                        ChatBubble(message)
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
                            // Removed .imePadding() so it stays at the bottom
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
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
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val userMsg = inputText
                                    messages = messages + ChatMessage(userMsg, true)
                                    inputText = ""

                                    if (messages.size <= 2) {
                                        chatTitle = if (userMsg.length > 15) userMsg.take(15) + "..." else userMsg
                                    }

                                    scope.launch {
                                        val response = chatService.getResponse(userMsg)
                                        messages = messages + ChatMessage(response, false)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = BrandColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
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
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
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