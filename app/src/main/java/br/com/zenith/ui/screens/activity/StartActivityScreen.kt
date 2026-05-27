package br.com.zenith.ui.screens.activity

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.TrackingResultHolder
import br.com.zenith.service.ActivityTrackingService
import br.com.zenith.service.TrackingStatus
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.activity.ActiveActivityViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.net.URLEncoder

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartActivityScreen(
    navController: NavController,
    exercicioId: String,
    exercicioNome: String,
    exercicioUnidade: String
) {
    ZenithTheme {
        val viewModel: ActiveActivityViewModel = viewModel()
        val context = LocalContext.current

        val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
        val status by viewModel.status.collectAsState()
        val steps by viewModel.steps.collectAsState()
        val distanceMeters by viewModel.distanceMeters.collectAsState()
        val routePoints by viewModel.routePoints.collectAsState()

        // Permissões necessárias
        val permissions = rememberMultiplePermissionsState(
            permissions = listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

        LaunchedEffect(Unit) {
            if (permissions.allPermissionsGranted) {
                viewModel.bindAndStart(context)
            } else {
                permissions.launchMultiplePermissionRequest()
            }
        }

        // Quando permissões forem concedidas, inicia
        LaunchedEffect(permissions.allPermissionsGranted) {
            if (permissions.allPermissionsGranted && status == TrackingStatus.IDLE) {
                viewModel.bindAndStart(context)
            }
        }

        LaunchedEffect(status) {
            if (status == TrackingStatus.STOPPED) {
                val result = viewModel.stop(context)
                TrackingResultHolder.result = result
                val duracaoMin = (result.duracaoSeconds / 60).coerceAtLeast(1)
                val nome = URLEncoder.encode(exercicioNome, "UTF-8")
                val unidade = URLEncoder.encode(exercicioUnidade, "UTF-8")
                navController.navigate("register_activity/$exercicioId/$nome/$unidade/$duracaoMin/true") {
                    popUpTo("start_activity/$exercicioId/$nome/$unidade") { inclusive = true }
                }
            }
        }

        // Tela de permissão negada
        if (!permissions.allPermissionsGranted) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Permissões necessárias",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "O Zenith precisa de acesso à localização e reconhecimento de atividade para rastrear seu treino.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            color = Color.Gray
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { permissions.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Conceder permissões", color = Color.White, fontFamily = Inter)
                    }
                }
            }
        } else {
            StartActivityContent(
                exercicioNome = exercicioNome,
                elapsedSeconds = elapsedSeconds,
                status = status,
                steps = steps,
                distanceMeters = distanceMeters,
                routePoints = routePoints,
                onPause = { viewModel.pause(context) },
                onResume = { viewModel.resume(context) },
                onStop = { viewModel.stop(context) }
            )
        }
    }
}

@Composable
fun StartActivityContent(
    exercicioNome: String,
    elapsedSeconds: Long,
    status: TrackingStatus,
    steps: Int,
    distanceMeters: Float,
    routePoints: List<LatLng>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val isRunning = status == TrackingStatus.RUNNING
    val isPaused = status == TrackingStatus.PAUSED

    var showStopDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    // Camera do mapa — segue o último ponto
    val lastPoint = routePoints.lastOrNull() ?: LatLng(-23.55, -46.63)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lastPoint, 17f)
    }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(routePoints.last(), 17f)
            )
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text("Finalizar atividade?", fontFamily = Inter, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "O rastreamento será parado e você poderá registrar os detalhes.",
                    fontFamily = Inter
                )
            },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false; onStop() }) {
                    Text("Finalizar", color = Color(0xFF238D25), fontFamily = Inter)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancelar", fontFamily = Inter)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = contentVisible,
            enter = slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { -it / 3 }
            )
        ) {
            // Mapa ocupa a metade superior
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF238D25),
                        width = 12f
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(340),
                initialOffsetY = { it }
            )
        ) {
            // Painel inferior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                .background(
                    if (isPaused) Color(0xFFF5F5F5) else Color.White,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Nome do exercício + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercicioNome,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF238D25)
                    )
                )
                Text(
                    text = if (isPaused) "Pausado" else "Em andamento",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color.Gray
                    )
                )
            }

            // Cronômetro
            Text(
                text = ActivityTrackingService.formatTime(elapsedSeconds),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp,
                    color = if (isPaused) Color(0xFFAAAAAA) else Color(0xFF238D25)
                )
            )

            // Stats: km e passos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Distância",
                    value = "%.2f km".format(distanceMeters / 1000f)
                )
                StatItem(
                    label = "Passos",
                    value = "$steps"
                )
            }

            // Botões
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEEEE))
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Finalizar",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { if (isRunning) onPause() else onResume() },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF238D25))
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Retomar",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = "Minimize o app para continuar rastreando",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color(0xFFBBBBBB)
                )
            )
        }
    }
}

}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color.Gray
            )
        )
    }
}
