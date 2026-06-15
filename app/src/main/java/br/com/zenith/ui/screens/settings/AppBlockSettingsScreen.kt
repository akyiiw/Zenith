package br.com.zenith.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.models.AppBlock
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithTimeField
import br.com.zenith.viewmodels.settings.AppBlockViewModel

@Composable
fun AppBlockSettingsScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: AppBlockViewModel = viewModel()
        val blocks by viewModel.blocks.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        var appName by rememberSaveable { mutableStateOf("") }
        var startTime by rememberSaveable { mutableStateOf("22:00") }
        var endTime by rememberSaveable { mutableStateOf("07:00") }
        var blockedDays by rememberSaveable { mutableStateOf("Todos os dias") }
        var reminder by rememberSaveable { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            viewModel.load(context)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = "Bloqueio de apps",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Nova janela",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Nome do app") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ZenithTimeField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = "Início",
                                modifier = Modifier.weight(1f)
                            )
                            ZenithTimeField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = "Fim",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = blockedDays,
                            onValueChange = { blockedDays = it },
                            label = { Text("Dias") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lembrete", fontFamily = Inter)
                            Switch(
                                checked = reminder,
                                onCheckedChange = { reminder = it },
                                colors = zenithSwitchColors()
                            )
                        }
                        Button(
                            onClick = {
                                if (appName.isBlank()) {
                                    ZenithNotifier.warning("Informe o nome do app.")
                                    return@Button
                                }
                                viewModel.create(
                                    appName = appName.trim(),
                                    startTime = startTime,
                                    endTime = endTime,
                                    blockedDays = blockedDays,
                                    reminder = reminder,
                                    context = context
                                )
                                appName = ""
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Adicionar", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                if (isLoading) {
                    item { CenteredZenithLoading() }
                } else if (blocks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum bloqueio configurado.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Inter,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                } else {
                    items(blocks, key = { it.id }) { block ->
                        AppBlockCard(
                            block = block,
                            enabled = !isSaving,
                            onActiveChange = { viewModel.setActive(block, it, context) },
                            onDelete = { viewModel.delete(block, context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBlockCard(
    block: AppBlock,
    enabled: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F7F1), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.appName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${block.startTime.take(5)} - ${block.endTime.take(5)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF238D25)
                )
            )
            Text(
                text = block.blockedDays?.takeIf { it.isNotBlank() } ?: "Dias não definidos",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color.Gray
                )
            )
        }
        Switch(
            checked = block.active,
            onCheckedChange = onActiveChange,
            enabled = enabled,
            colors = zenithSwitchColors()
        )
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remover",
                tint = Color(0xFFB3261E)
            )
        }
    }
}
