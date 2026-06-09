package br.com.zenith.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.notifications.ZenithNotifier
import io.github.jan.supabase.auth.auth

@Composable
fun VerifyEmailScreen(navController: NavController) {
    ZenithTheme {
        var isChecking by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "📧", fontSize = 64.sp)

                Text(
                    text = "Verifique\nseu email.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 40.sp
                    )
                )

                Text(
                    text = "Enviamos um link de confirmação para o seu email. Após confirmar, volte aqui para continuar.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Inter,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isChecking) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ZenithLoading(modifier = Modifier.size(52.dp), strokeWidth = 18f)
                    }
                } else {
                    Button(
                        onClick = {
                            isChecking = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // Força refresh da sessão para pegar estado atualizado
                                    val client = SupabaseConfig.getClient()
                                    client.auth.refreshCurrentSession()
                                    val user = client.auth.currentUserOrNull()
                                    val confirmado = user?.emailConfirmedAt != null

                                    withContext(Dispatchers.Main) {
                                        if (confirmado) {
                                            navController.navigate("onboarding") {
                                                popUpTo("verify_email") { inclusive = true }
                                            }
                                        } else {
                                            isChecking = false
                                            ZenithNotifier.warning(
                                                "Email ainda não confirmado. Verifique sua caixa de entrada."
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isChecking = false
                                        ZenithNotifier.error("Erro ao verificar: ${e.localizedMessage}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                    ) {
                        Text(
                            text = "Já confirmei!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.W600,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
