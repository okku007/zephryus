package com.zephyrus.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyrus.data.ConnectionState
import com.zephyrus.data.OperationState
import com.zephyrus.ui.theme.Danger
import com.zephyrus.ui.theme.Success
import com.zephyrus.ui.theme.Warning
import com.zephyrus.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val containerStatus by viewModel.containerStatus.collectAsState()
    val connectionLog by viewModel.connectionLog.collectAsState()
    val context = LocalContext.current
    
    // Show toast on operation result
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> {
                Toast.makeText(context, "Success: ${state.output.take(50)}", Toast.LENGTH_SHORT).show()
                viewModel.clearOperationState()
            }
            is OperationState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.clearOperationState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zephyrus", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            ConnectionCard(
                connectionState = connectionState,
                host = viewModel.host,
                containerName = viewModel.containerName,
                containerStatus = containerStatus,
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() }
            )
            
            // Connection Log Card (shows when there are logs)
            AnimatedVisibility(
                visible = connectionLog.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ConnectionLogCard(
                    logs = connectionLog,
                    onClear = { viewModel.clearLog() }
                )
            }
            
            // Operations Card
            AnimatedVisibility(
                visible = connectionState is ConnectionState.Connected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OperationsCard(
                    operationState = operationState,
                    onRestart = { viewModel.restartContainer() },
                    onStop = { viewModel.stopContainer() },
                    onStart = { viewModel.startContainer() },
                    onRefresh = { viewModel.refreshContainerStatus() }
                )
            }
            
            // Key Import Notice
            if (!viewModel.hasPrivateKey) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "No SSH key imported. Go to Settings to import your private key.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionLogCard(
    logs: List<String>,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Connection Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear log"
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    logs.forEach { log ->
                        val isError = log.contains("ERROR") || log.contains("Cause:")
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) 
                                MaterialTheme.colorScheme.error 
                            else if (log.contains("SUCCESS"))
                                Success
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    connectionState: ConnectionState,
    host: String,
    containerName: String,
    containerStatus: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Server Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // Status indicator
                StatusChip(connectionState)
            }
            
            HorizontalDivider()
            
            // Container info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Container",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        containerName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        containerStatus ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (containerStatus?.contains("Up") == true) Success else Warning
                    )
                }
            }
            
            // Connect/Disconnect button
            Button(
                onClick = {
                    when (connectionState) {
                        is ConnectionState.Connected -> onDisconnect()
                        is ConnectionState.Disconnected, is ConnectionState.Error -> onConnect()
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState !is ConnectionState.Connecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState is ConnectionState.Connected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                when (connectionState) {
                    is ConnectionState.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting...")
                    }
                    is ConnectionState.Connected -> {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Disconnect")
                    }
                    else -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
            }
            
            // Error message
            (connectionState as? ConnectionState.Error)?.let { error ->
                Text(
                    error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatusChip(state: ConnectionState) {
    val (color, text) = when (state) {
        is ConnectionState.Connected -> Success to "Connected"
        is ConnectionState.Connecting -> Warning to "Connecting"
        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline to "Disconnected"
        is ConnectionState.Error -> Danger to "Error"
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun OperationsCard(
    operationState: OperationState,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onStart: () -> Unit,
    onRefresh: () -> Unit
) {
    val isLoading = operationState is OperationState.InProgress
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Container Operations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Main restart button
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Restarting...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restart Container")
                }
            }
            
            // Secondary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
                
                OutlinedButton(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }
                
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Status")
                }
            }
        }
    }
}
