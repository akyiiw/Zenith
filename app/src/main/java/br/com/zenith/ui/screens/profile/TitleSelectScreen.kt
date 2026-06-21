package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
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
import br.com.zenith.ui.theme.Poppins
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
            .statusBarsPadding()
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
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF111111)
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
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (titulos.isEmpty()) {
                    item {
                        EmptyTitlesCard()
                    }
                } else {
                    item {
                        val currentTitle = titulos.firstOrNull { it.id == tituloAtualId }
                        TitleSummaryCard(currentTitle = currentTitle)
                    }
                }

                items(titulos, key = { it.id }) { titulo ->
                    val cor = remember(titulo.cor) {
                        try { Color(titulo.cor.toColorInt()) }
                        catch (e: Exception) { Color(0xFF238D25) }
                    }
                    val selecionado = titulo.id == tituloAtualId

                    TitleOptionCard(
                        titulo = titulo,
                        color = cor,
                        selected = selecionado,
                        onClick = { onSelect(titulo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleSummaryCard(currentTitle: Titulo?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Título ativo",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = Color(0xFF111111)
            )
        )
        if (currentTitle == null) {
            Text(
                text = "Nenhum título selecionado.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF6F6C6C)
                )
            )
        } else {
            val color = remember(currentTitle.cor) {
                try { Color(currentTitle.cor.toColorInt()) }
                catch (e: Exception) { Color(0xFF238D25) }
            }
            Text(
                text = currentTitle.nome,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.W800,
                    color = color
                )
            )
            currentTitle.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyTitlesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Nenhum título desbloqueado ainda",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = Color(0xFF111111)
            )
        )
        Text(
            text = "Continue usando o Zenith para liberar títulos no seu perfil.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color(0xFF6F6C6C)
            )
        )
    }
}

@Composable
private fun TitleOptionCard(
    titulo: Titulo,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = titulo.nome,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.W800,
                        color = color
                    )
                )
                Text(
                    text = titulo.descricao?.takeIf { it.isNotBlank() } ?: "Título desbloqueado",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(28.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

