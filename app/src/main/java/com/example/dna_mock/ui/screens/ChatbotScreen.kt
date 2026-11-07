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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dna_mock.ChatMessage
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor
import com.example.dna_mock.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(viewModel: ChatViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }

    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = BrandColor.copy(alpha = 0.1f)
                )
                if (viewModel.chatHistory.isEmpty()) {
                    Text(
                        text = "No previous chats",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                } else {
                    viewModel.chatHistory.forEach { chat ->
                        NavigationDrawerItem(
                            label = { Text(text = chat.title, maxLines = 1) },
                            selected = chat.id == viewModel.conversationId,
                            onClick = {
                                viewModel.openChat(chat)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = viewModel.chatTitle, color = BackgroundColor) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History", tint = BackgroundColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = BackgroundColor)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Chat") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.newChat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearChat()
                                }
                            )
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
                    items(messages.size) { index ->
                        val message = messages[index]
                        ChatBubble(
                            message = message,
                            onConsentClick = { granted -> viewModel.sendConsent(granted, message.consentRequestedFor) },
                            onEscalateClick = { viewModel.talkToPerson(message.escalationTarget) },
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
fun ChatBubble(
    message: ChatMessage,
    onConsentClick: (Boolean) -> Unit,
    onEscalateClick: () -> Unit,
    onViewJsonClicked: (String) -> Unit
) {
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
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Display text for every reply
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Consent Request UI: Allow / Don't allow buttons
                if (!isUser && message.replyType == "consent_request") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConsentClick(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Allow", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { onConsentClick(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Don't allow", color = Color.Black)
                        }
                    }
                }

                // Final Answer Steps UI: Checklist
                if (!isUser && message.steps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Action Steps:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandColor
                    )
                    message.steps.forEach { stepText ->
                        ChecklistStepItem(stepText = stepText)
                    }
                }

                // Escalation Required UI: "Talk to a person" button
                if (!isUser && message.escalationRequired) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onEscalateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Talk to a person", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Raw JSON Inspector Trigger
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
fun ChecklistStepItem(stepText: String) {
    var checked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checked = !checked }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = CheckboxDefaults.colors(checkedColor = BrandColor)
        )
        Text(
            text = stepText,
            style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (checked) Color.Gray else Color.DarkGray
        )
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