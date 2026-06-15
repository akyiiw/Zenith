package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.models.Titulo
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.UserViewModel
import androidx.core.graphics.toColorInt

@Composable
fun TitleSelectScreen(navController: NavController) {
    ZenithTheme {
        val userViewModel: UserViewModel = viewModel()
        val context = LocalContext.current
        val titulos by userViewModel.titulosDisponiveis.collectAsState()
        val tituloAtual by userViewModel.userState.collectAsState()
        val isLoading by userViewModel.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            if (tituloAtual == null) userViewModel.fetchUserProfile(context) // â† garante que o estado existe
            userViewModel.fetchTitulosDisponiveis(context)
        }

        TitleSelectContent(
            titulos = titulos,
            tituloAtualId = tituloAtual?.tituloId,
            isLoading = isLoading,
            onSelect = { titulo ->
                userViewModel.selecionarTitulo(titulo.id, context) {
                    navController.popBackStack()
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun TitleSelectContent(
    titulos: List<Titulo>,
    tituloAtualId: String?,
    isLoading: Boolean,
    onSelect: (Titulo) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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
                text = "Escolher título",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isLoading) {
            CenteredZenithLoading()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp),
                contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (titulos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum título desbloqueado ainda.",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                            )
                        }
                    }
                }

                items(titulos) { titulo ->
                    val cor = remember(titulo.cor) {
                        try { Color(titulo.cor.toColorInt()) }
                        catch (e: Exception) { Color(0xFF580C83) }
                    }
                    val selecionado = titulo.id == tituloAtualId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selecionado) cor.copy(alpha = 0.08f) else Color(0xFFF5F5F5),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (selecionado) 1.5.dp else 0.5.dp,
                                color = if (selecionado) cor else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { onSelect(titulo) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = titulo.nome,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.W600,
                                    color = cor
                                )
                            )
                            titulo.descricao?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                        }
                        if (selecionado) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = cor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

