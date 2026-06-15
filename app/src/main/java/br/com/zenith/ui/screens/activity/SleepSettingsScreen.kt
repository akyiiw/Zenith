package br.com.zenith.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.sleep.SleepDataSource
import br.com.zenith.data.sleep.SleepDetectionPlan
import br.com.zenith.data.sleep.SleepMonitoringSettings
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithTimeWheel
import br.com.zenith.viewmodels.sleep.SleepSettingsViewModel

@Composable
fun SleepSettingsScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: SleepSettingsViewModel = viewModel()
        val settings by viewModel.settings.collectAsState()
        val detectionPlan by viewModel.detectionPlan.collectAsState()

        SleepSettingsContent(
            settings = settings,
            detectionPlan = detectionPlan,
            onSettingsChange = { transform -> viewModel.updateSettings(transform) },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun SleepSettingsContent(
    settings: SleepMonitoringSettings,
    detectionPlan: SleepDetectionPlan,
    onSettingsChange: ((SleepMonitoringSettings) -> SleepMonitoringSettings) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = "Configurações de sono",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, top = 12.dp, end = 24.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SleepHeaderCard(settings = settings)
                }

                item {
                    SettingsSwitchCard(
                        title = "Monitoramento de sono",
                        description = if (settings.enabled) {
                            "Ativo para detectar sono dentro da janela configurada."
                        } else {
                            "Desativado. O sono poderá ser registrado manualmente."
                        },
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            onSettingsChange { it.copy(enabled = enabled) }
                        }
                    )
                }

                item {
                    RestWindowCard(
                        settings = settings,
                        onWindowChange = { start, end ->
                            onSettingsChange {
                                it.copy(
                                    restWindowStartMinutes = start,
                                    restWindowEndMinutes = end
                                )
                            }
                        }
                    )
                }

                item {
                    SectionTitle("Fontes de detecção")
                }

                item {
                    SettingsSwitchCard(
                        title = "Health Connect",
                        description = "Primeira opção quando houver sessão de sono de relógios, pulseiras ou apps de saúde.",
                        checked = settings.useHealthConnect,
                        enabled = settings.enabled,
                        onCheckedChange = { checked ->
                            onSettingsChange { it.copy(useHealthConnect = checked) }
                        }
                    )
                }

                item {
                    SettingsSwitchCard(
                        title = "Sleep API",
                        description = "Segunda opção para inferir sono pelo reconhecimento de atividade do Android/Google Play Services.",
                        checked = settings.useSleepApi,
                        enabled = settings.enabled,
                        onCheckedChange = { checked ->
                            onSettingsChange { it.copy(useSleepApi = checked) }
                        }
                    )
                }

                item {
                    SettingsSwitchCard(
                        title = "Estimativa do aparelho",
                        description = "Fallback baseado em tela ligada/desligada, desbloqueios e janela de descanso.",
                        checked = settings.useDeviceEstimate,
                        enabled = settings.enabled,
                        onCheckedChange = { checked ->
                            onSettingsChange { it.copy(useDeviceEstimate = checked) }
                        }
                    )
                }

                item {
                    DetectionPlanCard(detectionPlan = detectionPlan)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SleepHeaderCard(settings: SleepMonitoringSettings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFEAF1FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD2DDF5))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(15.dp),
                color = Color.White.copy(alpha = 0.82f)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color(0xFF2F5FBA),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = settings.restWindowLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                )
                Text(
                    text = if (settings.enabled) "Monitoramento ativo" else "Monitoramento desativado",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF555555)
                    )
                )
            }
        }
    }
}

@Composable
private fun RestWindowCard(
    settings: SleepMonitoringSettings,
    onWindowChange: (Int, Int) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { pickerOpen = true },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF7F7F7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Janela de descanso",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                )
                Text(
                    text = "Período em que o Zenith considera sono possível.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF666666)
                    )
                )
            }
            Text(
                text = settings.restWindowLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF238D25)
                )
            )
        }
    }

    if (pickerOpen) {
        RestWindowDialog(
            initialStart = settings.restWindowStartMinutes,
            initialEnd = settings.restWindowEndMinutes,
            onDismiss = { pickerOpen = false },
            onConfirm = { start, end ->
                onWindowChange(start, end)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun RestWindowDialog(
    initialStart: Int,
    initialEnd: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startHour by remember { mutableIntStateOf(initialStart / 60) }
    var startMinute by remember { mutableIntStateOf(initialStart % 60) }
    var endHour by remember { mutableIntStateOf(initialEnd / 60) }
    var endMinute by remember { mutableIntStateOf(initialEnd % 60) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(primary = Green),
        typography = Typography()
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Janela de descanso",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    TimeRow(
                        label = "Início",
                        hour = startHour,
                        minute = startMinute,
                        onHourChange = { startHour = it },
                        onMinuteChange = { startMinute = it }
                    )
                    TimeRow(
                        label = "Fim",
                        hour = endHour,
                        minute = endMinute,
                        onHourChange = { endHour = it },
                        onMinuteChange = { endMinute = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    startHour * 60 + startMinute,
                                    endHour * 60 + endMinute
                                )
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label: ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZenithTimeWheel(
                label = "Horas",
                value = hour,
                range = 0..23,
                onValueChange = onHourChange,
                modifier = Modifier.weight(1f)
            )
            ZenithTimeWheel(
                label = "Minutos",
                value = minute,
                range = 0..59,
                onValueChange = onMinuteChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) Color(0xFFF7F7F7) else Color(0xFFF1F1F1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color(0xFF151515) else Color.Gray
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF666666)
                    )
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = zenithSwitchColors()
            )
        }
    }
}

@Composable
private fun DetectionPlanCard(detectionPlan: SleepDetectionPlan) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Prioridade de uso",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF151515)
                )
            )
            detectionPlan.sources.forEachIndexed { index, source ->
                SourcePriorityRow(position = index + 1, source = source)
                if (index < detectionPlan.sources.lastIndex) {
                    HorizontalDivider(color = Color(0xFFE9E9E9))
                }
            }
            if (detectionPlan.interruptionPenaltyEnabled) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEAF3DE)
                ) {
                    Text(
                        text = "Desbloqueios dentro da janela de descanso serão contados como interrupções.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Inter,
                            color = Color(0xFF27500A)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcePriorityRow(position: Int, source: SleepDataSource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFEAF3DE), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF238D25)
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = source.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF151515)
                )
            )
            Text(
                text = source.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color(0xFF666666)
                )
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF555555)
        )
    )
}
