package br.com.zenith.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.utils.UnitFormatters

@Composable
fun ActivityRegisteredScreen(
    navController: NavController,
    exercicioNome: String,
    valor: String,
    unidade: String,
    duracaoMin: Int,
    verificada: Boolean
) {
    val displayValue = formatActivityRegisteredValue(valor)

    ZenithTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = Color(0xFFEAF3DE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (verificada) Icons.Default.Verified else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF238D25),
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Atividade registrada",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (verificada) "Atividade concluída e verificada." else "Registro manual salvo.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            color = Color.Gray
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = exercicioNome.ifBlank { "Exercício" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF238D25)
                        )
                    )
                    Text(
                        text = "$displayValue $unidade",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B820E)
                        )
                    )
                    if (duracaoMin > 0) {
                        ActivityRegisteredRow(label = "Duração", value = "${duracaoMin}min")
                    }
                    ActivityRegisteredRow(
                        label = "Tipo",
                        value = if (verificada) "Verificada" else "Manual"
                    )
                }

                Button(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        text = "Fechar",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600
                        )
                    )
                }
            }
        }
    }
}

private fun formatActivityRegisteredValue(value: String): String {
    val normalized = value.replace(",", ".")
    return normalized.toDoubleOrNull()?.let { UnitFormatters.compactNumber(it) } ?: value
}

@Composable
private fun ActivityRegisteredRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color.Gray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W600,
                color = Color(0xFF1A1A1A)
            )
        )
    }
}
