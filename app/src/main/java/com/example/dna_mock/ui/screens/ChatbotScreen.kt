package com.example.dna_mock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dna_mock.ChatMessage
import com.example.dna_mock.DemoScenario
import com.example.dna_mock.ui.UiStrings
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor
import com.example.dna_mock.ui.viewmodels.ChatHistoryItem
import com.example.dna_mock.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(viewModel: ChatViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }

    // confirmation dialogs
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChatHistoryItem?>(null) }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()
    val activeId by viewModel.activeConversationId.collectAsState()
    val history by viewModel.chatHistory.collectAsState()
    val scenarios by viewModel.scenarios.collectAsState()
    val selectedScenarioId by viewModel.selectedScenarioId.collectAsState()
    val language by viewModel.activeLanguage.collectAsState()

    val selectedScenario = scenarios.firstOrNull { it.id == selectedScenarioId }
    val canSend = !isLoading && viewModel.inputText.isNotBlank()

    LaunchedEffect(activeId, messages.size, isLoading) {
        val count = messages.size + if (isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    // ---- dialogs -----------------------------------------------------------------------

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear this chat?",
            message = "All messages in this chat are removed and the assistant forgets the case. This can't be undone.",
            confirmLabel = "Clear",
            onConfirm = { showClearDialog = false; viewModel.clearChat() },
            onDismiss = { showClearDialog = false }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete chat?",
            message = "\"${target.title}\" will be removed from your history.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteChat(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
    if (showDeleteAllDialog) {
        ConfirmDialog(
            title = "Delete all history?",
            message = "Every saved chat is removed. This can't be undone.",
            confirmLabel = "Delete all",
            onConfirm = { showDeleteAllDialog = false; viewModel.deleteAllChats(); scope.launch { drawerState.close() } },
            onDismiss = { showDeleteAllDialog = false }
        )
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
                NavigationDrawerItem(
                    label = { Text("New chat", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        viewModel.newChat()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = BrandColor.copy(alpha = 0.1f)
                )
                if (history.isEmpty()) {
                    Text(
                        text = "No previous chats",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(history, key = { it.id }) { chat ->
                            NavigationDrawerItem(
                                label = { Text(text = chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                selected = chat.id == activeId,
                                onClick = {
                                    viewModel.openChat(chat)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                badge = {
                                    IconButton(onClick = { deleteTarget = chat }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete chat", tint = Color.Gray)
                                    }
                                }
                            )
                        }
                    }
                    TextButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                    ) {
                        Text("Delete all history", color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = chatTitle, color = BackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
                                enabled = messages.isNotEmpty(),
                                onClick = {
                                    menuExpanded = false
                                    viewModel.newChat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                enabled = messages.isNotEmpty(),
                                onClick = {
                                    menuExpanded = false
                                    showClearDialog = true
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
                if (messages.isEmpty() && !isLoading) {
                    WelcomePane(
                        scenarios = scenarios,
                        selectedId = selectedScenarioId,
                        onSelect = { viewModel.selectScenario(it) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                actionsEnabled = !isLoading,
                                onConsentClick = { granted -> viewModel.sendConsent(message.id, granted) },
                                onEscalateClick = { viewModel.talkToPerson(message.escalationTarget) },
                                onViewJsonClicked = { json -> viewModel.showRawJson(json) }
                            )
                        }
                        if (isLoading) {
                            item(key = "typing") { TypingIndicator() }
                        }
                    }
                }

                // Input Area
                Surface(
                    color = BackgroundColor,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (selectedScenario != null) {
                            ScenarioBanner(
                                scenario = selectedScenario,
                                removable = messages.isEmpty(),
                                onRemove = { viewModel.selectScenario(null) }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = viewModel.inputText,
                                onValueChange = { viewModel.updateInputText(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(UiStrings.inputHint(language)) },
                                textStyle = TextStyle(color = Color.Black),
                                maxLines = 4,
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
                            IconButton(onClick = { viewModel.sendMessage() }, enabled = canSend) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) BrandColor else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = Color(0xFFD32F2F)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Shown while a chat is empty: explains the demo signals and lets the tester pick a scenario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomePane(
    scenarios: List<DemoScenario>,
    selectedId: String?,
    onSelect: (DemoScenario?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = null, tint = BrandColor, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "How can I help with a security concern?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandColor,
            textAlign = TextAlign.Center
        )
        if (scenarios.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Demo scenarios attach operator-side data (call warning, subscription, device, account) " +
                    "to your first message. This is what lets the assistant ask for your consent to use it.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            scenarios.forEach { scenario ->
                FilterChip(
                    selected = scenario.id == selectedId,
                    onClick = { onSelect(if (scenario.id == selectedId) null else scenario) },
                    label = { Text(scenario.title) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ScenarioBanner(scenario: DemoScenario, removable: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Signals attached: ${scenario.title}",
            style = MaterialTheme.typography.labelMedium,
            color = BrandColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (removable) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove signals", tint = BrandColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        ProfilePicture(isAI = true)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = Color(0xFFE8E1E5),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(12.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = BrandColor
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    actionsEnabled: Boolean,
    onConsentClick: (Boolean) -> Unit,
    onEscalateClick: () -> Unit,
    onViewJsonClicked: (String) -> Unit
) {
    // Local system note (e.g. "session expired"): centered, no bubble
    if (message.replyType == "note") {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val isUser = message.isUser
    val lang = message.language
    val textColor = when {
        isUser -> Color.White
        message.isError -> Color(0xFFB71C1C)
        else -> Color.Black
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) ProfilePicture(isAI = true)
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = when {
                isUser -> BrandColor
                message.isError -> Color(0xFFFFEBEE)
                else -> Color(0xFFE8E1E5)
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Risk badge on final answers
                if (!isUser && message.replyType == "final" && message.riskLevel != null) {
                    RiskBadge(label = UiStrings.risk(lang, message.riskLevel), level = message.riskLevel)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Display text for every reply
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Consent Request UI: Allow / Don't allow buttons (disabled once answered)
                if (!isUser && message.replyType == "consent_request") {
                    Spacer(modifier = Modifier.height(10.dp))
                    val enabled = actionsEnabled && !message.consentAnswered
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConsentClick(true) },
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(UiStrings.allow(lang), color = Color.White, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onConsentClick(false) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(UiStrings.deny(lang), color = Color.Black, maxLines = 1)
                        }
                    }
                }

                // Final Answer Steps UI: Checklist
                if (!isUser && message.steps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        UiStrings.actionSteps(lang),
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
                        Text(UiStrings.talkToPerson(lang), color = Color.White, fontWeight = FontWeight.Bold)
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
private fun RiskBadge(label: String, level: String) {
    val color = when (level) {
        "none", "low" -> Color(0xFF2E7D32)
        "medium" -> Color(0xFFEF6C00)
        "high" -> Color(0xFFD84315)
        "critical" -> Color(0xFFB71C1C)
        else -> Color.Gray
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ChecklistStepItem(stepText: String) {
    // rememberSaveable: the tick survives scrolling the bubble off-screen and rotation
    var checked by rememberSaveable { mutableStateOf(false) }

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
