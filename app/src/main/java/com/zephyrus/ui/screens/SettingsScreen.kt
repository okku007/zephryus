package com.zephyrus.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zephyrus.ui.theme.Success
import com.zephyrus.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val context = LocalContext.current
    
    var host by remember { mutableStateOf(viewModel.host) }
    var port by remember { mutableStateOf(viewModel.port.toString()) }
    var username by remember { mutableStateOf(viewModel.username) }
    var containerName by remember { mutableStateOf(viewModel.containerName) }
    
    // File picker for SSH key - OpenDocument shows all files including extensionless
    val keyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val keyBytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (keyBytes != null) {
                    viewModel.importPrivateKey(keyBytes)
                    Toast.makeText(context, "SSH key imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import key: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            // Server Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Server Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = host,
                        onValueChange = { 
                            host = it
                            viewModel.updateHost(it)
                        },
                        label = { Text("Host / IP Address") },
                        placeholder = { Text("192.168.2.102") },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = port,
                        onValueChange = { 
                            port = it
                            it.toIntOrNull()?.let { p -> viewModel.updatePort(p) }
                        },
                        label = { Text("Port") },
                        placeholder = { Text("22") },
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            viewModel.updateUsername(it)
                        },
                        label = { Text("Username") },
                        placeholder = { Text("root") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = containerName,
                        onValueChange = { 
                            containerName = it
                            viewModel.updateContainerName(it)
                        },
                        label = { Text("Docker Container Name") },
                        placeholder = { Text("blogsite") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            // SSH Key Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SSH Private Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (viewModel.hasPrivateKey) {
                            Surface(
                                color = Success.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Success
                                    )
                                    Text(
                                        "Imported",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Success
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        "Import your SSH private key file (id_rsa, id_ed25519, etc.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Button(
                        onClick = { keyPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (viewModel.hasPrivateKey) "Replace SSH Key" else "Import SSH Key")
                    }
                }
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Setup Requirements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        "• SSH user must be in the 'docker' group\n" +
                        "• Private key must match the public key on server\n" +
                        "• Server must be reachable on the network",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
