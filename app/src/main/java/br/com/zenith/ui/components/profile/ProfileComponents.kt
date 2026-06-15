package br.com.zenith.ui.components.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.zenith.R
import androidx.compose.foundation.pager.PageSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.theme.Inter
import br.com.zenith.data.models.Badge
import br.com.zenith.data.models.Profile
import br.com.zenith.data.models.Titulo
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.UserStats
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ChallengeAward
import br.com.zenith.data.models.ChallengeAwardCalculator
import br.com.zenith.data.models.ChallengeAwardSummary
import br.com.zenith.data.models.ChallengeMedalType
import br.com.zenith.data.models.Desafio
import br.com.zenith.utils.UnitFormatters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

private data class BadgeConfig(
    val backgroundColor: Color,
    val iconRes: Int,
    val textColor: Color = Color.Black,
    val tint: Color = Color.Black,
    val iconSize: androidx.compose.ui.unit.Dp = 24.dp,
)

private fun badgeConfig(title: String): BadgeConfig? = when (title.uppercase()) {
    "DEV" -> BadgeConfig(
        backgroundColor = Color(0xFF5DA8FE),
        iconRes = R.drawable.badge_dev,
    )
    "TESTER" -> BadgeConfig(
        backgroundColor = Color(0xFFD79B3B),
        iconRes = R.drawable.badge_tester,
        iconSize = 20.dp
    )
    "PREMIUM" -> BadgeConfig(
        backgroundColor = Color(0xFF4CAF50),
        iconRes = R.drawable.badge_premium,
        iconSize = 20.dp
    )
    else -> null
}

@Composable
fun ProfileHeader(
    user: Profile?,
    stats: UserStats,
    badge: Badge?,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onTitleClick: () -> Unit = {},
    onStatusClick: (() -> Unit)? = null,
    onBadgeClick: (() -> Unit)? = null,
    onStatClick: (String) -> Unit = {}
) {
    ZenithTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Banner(
                    bannerHash = user?.bannerHash
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBack() }
                    )
                    if (onEdit != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.badge_dev),
                            contentDescription = "Editar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x88FFFFFF))
                                .border(1.dp, Color(0x88000000), RoundedCornerShape(6.dp))
                                .clickable { onEdit() }
                                .padding(6.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, top = 65.dp)
                ) {
                    ProfilePicture(pictureHash = user?.pictureHash)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 100.dp, y = (-25).dp)
                    ) {
                        Status(status = user?.status, onClick = onStatusClick)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            UserSection(
                user = user,
                badge = badge,
                onTitleClick = onTitleClick,
                onBadgeClick = onBadgeClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            Streak(streak = user?.streak ?: 0)
            Spacer(modifier = Modifier.height(8.dp))
            Stats(stats = stats, onStatClick = onStatClick)
        }
    }
}

@Composable
fun UserSection(
    user: Profile?,
    badge: Badge?,
    onTitleClick: () -> Unit,
    onBadgeClick: (() -> Unit)? = null
) {
    fun formatDate(isoDate: String): String {
        return try {
            val input = java.time.OffsetDateTime.parse(isoDate)
            "%02d/%02d/%d".format(input.dayOfMonth, input.monthValue, input.year)
        } catch (e: Exception) {
            isoDate
        }
    }

    Column(modifier = Modifier.padding(start = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user?.displayName ?: "Carregando...",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(6.dp))
            Badge(badge = badge, onClick = onBadgeClick)
        }
        Text(
            text = "@${user?.name ?: "..."}",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (user?.registerDate != null) {
                "Membro desde ${formatDate(user.registerDate)}"
            } else {
                "Carregando..."
            },
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Title(titulo = user?.titulo, onClick = onTitleClick)
    }
}

@Composable
fun Badge(badge: Badge?, onClick: (() -> Unit)? = null) {
    if (badge == null) {
        if (onClick == null) return

        Row(
            modifier = Modifier
                .background(color = Color(0xFFEAF3DE), shape = RoundedCornerShape(size = 15.dp))
                .wrapContentSize()
                .clickable { onClick() }
                .padding(start = 10.dp, top = 3.dp, end = 10.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Badge", color = Color(0xFF238D25))
        }
        return
    }

    val config = badgeConfig(badge.title) ?: return  // ← some silenciosamente

    Row(
        modifier = Modifier
            .background(color = config.backgroundColor, shape = RoundedCornerShape(size = 15.dp))
            .wrapContentSize()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(start = 10.dp, top = 3.dp, end = 10.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = config.iconRes),
            contentDescription = badge.title,
            tint = Color.Unspecified,
            modifier = Modifier.size(config.iconSize)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = badge.title, color = config.textColor)
    }
}

@Composable
fun RecentHeader(onViewAll: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Atividades recentes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W600
            )
            Text(
                text = "Ver todas",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W600,
                color = Color(0x99111111),
                modifier = Modifier.then(
                    if (onViewAll != null) Modifier.clickable { onViewAll() } else Modifier
                )
            )
        }
    }
}

@Composable
fun AboutSection(
    aboutMe: String?,
    isOwnProfile: Boolean,
    onEdit: () -> Unit = {}
) {
    val text = aboutMe?.trim().orEmpty()
    if (text.isBlank() && !isOwnProfile) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 6.dp)
            .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(size = 5.dp))
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(size = 5.dp))
            .then(
                if (text.isBlank()) {
                    Modifier.clickable { onEdit() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Sobre",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.W600,
            color = Color(0xFF000000)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text.ifBlank { "Adicione uma bio ao seu perfil" },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = if (text.isBlank()) Color(0xFF6F6C6C) else Color(0xFF2A2A2A)
            )
        )
    }
}

fun tempoRelativo(dataIso: String?): String {
    return try {
        val atividadeTime = java.time.OffsetDateTime.parse(dataIso)
        val agora = java.time.OffsetDateTime.now()
        val duracao = java.time.Duration.between(atividadeTime, agora)
        val minutos = duracao.toMinutes()
        val horas = duracao.toHours()
        val dias = duracao.toDays()
        when {
            minutos < 1 -> "Agora mesmo"
            minutos < 60 -> "Há $minutos minuto(s)"
            horas < 24 -> "Há $horas hora(s)"
            else -> "Há $dias dia(s)"
        }
    } catch (e: Exception) {
        ""
    }
}

fun activityType(atividade: Atividade): String {
    return atividade.exercicio?.nome ?: "Atividade"
}

fun activityGroup(atividade: Atividade): String {
    atividade.exercicio?.grupo?.takeIf { it.isNotBlank() }?.let { return it }

    val text = "${atividade.exercicio?.slug.orEmpty()} ${atividade.exercicio?.nome.orEmpty()}".lowercase()
    return when {
        "caminh" in text -> "Caminhadas com meus amigos"
        "corr" in text -> "Corridas"
        "sono" in text || "dorm" in text -> "Sono e descanso"
        "bike" in text || "cicl" in text -> "Pedaladas"
        "academia" in text || "muscul" in text -> "Treinos de forca"
        else -> "Atividades pessoais"
    }
}

fun formattedActivityValue(atividade: Atividade): String {
    val text = "${atividade.exercicio?.slug.orEmpty()} ${atividade.exercicio?.nome.orEmpty()} ${atividade.exercicio?.unidade.orEmpty()}".lowercase()
    val value = atividade.valor
    return when {
        "pass" in text -> "${UnitFormatters.steps(value.toInt())} passos"
        "km" in text || "corr" in text || "caminh" in text || "bike" in text || "cicl" in text ->
            UnitFormatters.kilometersWithSpace(value)
        "min" in text ->
            UnitFormatters.minutes(value.toInt())
        else ->
            UnitFormatters.compactNumber(value) + "h"
    }
}

fun formattedDuration(duracaoMin: Int?): String {
    return UnitFormatters.minutes(duracaoMin)
}

@Composable
fun RecentActivitySection(
    navController: NavController,
    atividades: List<Atividade>,
    desafios: Map<String, Desafio> = emptyMap()
) {
    val recent = remember(atividades) {
        atividades.sortedByDescending { it.realizadaEm }.take(5)
    }
    Column {
        recent.forEach { atividade ->
            RecentActivityCard(
                atividade = atividade,
                desafio = atividade.desafioId?.let { desafios[it] },
                onClick = { navController.navigate("activity_detail/${atividade.id}") }
            )
        }
    }
}

@Composable
fun RecentActivityCard(atividade: Atividade, desafio: Desafio? = null, onClick: () -> Unit) {
    ZenithTheme {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, top = 10.dp, end = 32.dp)
                .fillMaxWidth()
                .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(size = 5.dp))
                .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(size = 5.dp))
                .clickable { onClick() }
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tempoRelativo(atividade.realizadaEm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                        color = Color(0xFF000000),
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = activityType(atividade),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W600,
                        color = Color(0xFF238D25)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activityGroup(atividade),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6F6C6C)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = atividade.titulo ?: "Atividade registrada:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W600,
                        color = Color(0xFF000000)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedActivityValue(atividade),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 27.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.W700,
                        ),
                        color = Color(0xFF1B820E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Duracao: ${formattedDuration(atividade.duracaoMin)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        color = Color(0xFF6F6C6C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nota do usuario: ${atividade.nota?.let { "$it/10" } ?: "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        color = Color(0xFF6F6C6C)
                    )
                }
                if (atividade.verificada || !atividade.rota.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.width(92.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActivityStatusBadge(verificada = atividade.verificada)
                        RouteSparkline(rota = atividade.rota)
                    }
                }
            }
            ChallengeActivitySummary(desafio = desafio, atividade = atividade)
        }
    }
}

@Composable
private fun ActivityStatusBadge(verificada: Boolean) {
    if (!verificada) return

    Box(
        modifier = Modifier
            .padding(1.dp)
            .width(24.dp)
            .height(16.dp)
            .background(color = Color(0x5CC9C9C9), shape = RoundedCornerShape(size = 25.dp))
    ) {
        Icon(
            painter = painterResource(
                id = R.drawable.act_verified
            ),
            contentDescription = "Status da Atividade",
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(start = 3.dp, top = 3.dp, bottom = 3.dp)
                .size(18.dp)
        )
    }
}

@Composable
fun ChallengeActivitySummary(desafio: Desafio?, atividade: Atividade) {
    if (atividade.desafioId.isNullOrBlank()) return

    Spacer(modifier = Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF3DE), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0x33238D25), RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = desafio?.titulo?.takeIf { it.isNotBlank() } ?: "Desafio vinculado",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = Color(0xFF238D25)
        )
        Text(
            text = desafio?.let { "${challengeModeLabel(it)} - conta para o ranking" }
                ?: "Conta para o ranking do desafio",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = Color(0xFF4F664F)
        )
        val status = when {
            atividade.gpsQualidade == "ruim" -> "GPS ruim"
            atividade.verificada -> "Atividade verificada"
            else -> "Registro manual"
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = Color(0xFF6F6C6C)
        )
    }
}

fun challengeModeLabel(challenge: Desafio): String {
    val objective = challenge.objetivoValor ?: challenge.meta
    return when (challenge.rankingTipo) {
        "menor_tempo" -> "${UnitFormatters.compactNumber(objective)} km - menor tempo"
        "maior_distancia" -> "${UnitFormatters.minutes(objective.toInt())} - maior distancia"
        "menor_pace" -> "${UnitFormatters.compactNumber(objective)} km - menor pace"
        "tempo_total" -> "tempo total"
        "distancia_total" -> "distancia total"
        else -> "${challenge.unidade.lowercase(Locale.ROOT)} no ranking"
    }
}

@Composable
private fun RouteSparkline(rota: String?) {
    val points = remember(rota) { parseRouteSparklinePoints(rota) }
    if (points.size < 2) return

    Spacer(modifier = Modifier.height(14.dp))
    Canvas(
        modifier = Modifier
            .width(90.dp)
            .height(56.dp)
            .padding(4.dp)
    ) {
        val minLat = points.minOf { it.first }
        val maxLat = points.maxOf { it.first }
        val minLng = points.minOf { it.second }
        val maxLng = points.maxOf { it.second }
        val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 1.0
        val lngRange = (maxLng - minLng).takeIf { it != 0.0 } ?: 1.0
        val scale = minOf(
            size.width / lngRange.toFloat(),
            size.height / latRange.toFloat()
        )
        val drawnWidth = lngRange.toFloat() * scale
        val drawnHeight = latRange.toFloat() * scale
        val offsetX = (size.width - drawnWidth) / 2f
        val offsetY = (size.height - drawnHeight) / 2f

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = offsetX + ((point.second - minLng).toFloat() * scale)
            val y = offsetY + drawnHeight - ((point.first - minLat).toFloat() * scale)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF6B6B6B),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

private fun parseRouteSparklinePoints(rota: String?): List<Pair<Double, Double>> {
    if (rota.isNullOrBlank()) return emptyList()
    return runCatching {
        Json.parseToJsonElement(rota).jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
            val lng = obj["lng"]?.jsonPrimitive?.doubleOrNull
            if (lat != null && lng != null) lat to lng else null
        }
    }.getOrDefault(emptyList())
}

@Composable
fun Title(titulo: Titulo?, onClick: () -> Unit = {}) {
    if (titulo != null) {
        val cor = remember(titulo.cor) {
            try { Color(titulo.cor.toColorInt()) }
            catch (e: Exception) { Color(0xFF580C83) }
        }
        Text(
            text = titulo.nome,
            style = MaterialTheme.typography.labelLarge,
            color = cor,
            fontWeight = FontWeight.W600,
            modifier = Modifier.clickable { onClick() }
        )
    } else {
        Text(
            text = "Escolher título",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF238D25).copy(alpha = 0.6f),
            fontWeight = FontWeight.W400,
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
fun Streak(streak: Int) {
    Row(
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp)
            .fillMaxWidth()
            .background(color = Color(0x29D2D4D2), shape = RoundedCornerShape(size = 12.dp))
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sequência",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF000000),
            fontWeight = FontWeight.W600,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                painter = painterResource(id = R.drawable.firestreak),
                contentDescription = "streak",
                tint = Color(0xFFF26500)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = streak.toString(),
                color = Color(0xFFF26500),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W600
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.greendot),
            contentDescription = "arrow",
            tint = Color(0xFFF26500)
        )
    }
}

@Composable
fun Banner(bannerHash: String?) {
    if (bannerHash != null) {
        val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(bannerHash)
        AsyncImage(
            model = url,
            contentDescription = "Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(color = Color(0xFFB5B5B5))
        )
    }
}

@Composable
fun ProfilePicture(pictureHash: String?) {
    if (pictureHash != null) {
        val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(pictureHash)
        AsyncImage(
            model = url,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(135.dp)
                .clip(CircleShape)
                .border(5.dp, Color.White, CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.profile_picture),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(135.dp)
                .clip(CircleShape)
                .border(5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun Stats(stats: UserStats, onStatClick: (String) -> Unit = {}) {
    val statItems = listOf(
        "Desafios" to stats.desafios,
        "Amigos" to stats.amigos,
        "Conquistas" to stats.conquistas
    )
    val pagerState = rememberPagerState(pageCount = { statItems.size })
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(start = 32.dp, top = 5.dp, end = 32.dp),
        pageSpacing = 12.dp,
        pageSize = PageSize.Fixed(pageSize = 110.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp)
    ) { page ->
        val (type, number) = statItems[page]
        StatCard(type = type, number = number, onClick = { onStatClick(type) })
    }
}

@Composable
fun ChallengeAwardsSection(
    summary: ChallengeAwardSummary,
    historyLimit: Int? = 5,
    onAwardClick: (ChallengeAward) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChallengePodiumSection(summary = summary)
        ChallengeAwardHistorySection(
            summary = summary,
            historyLimit = historyLimit,
            onAwardClick = onAwardClick
        )
    }
}

@Composable
fun ChallengePodiumSection(summary: ChallengeAwardSummary) {
    Column(
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp, top = 10.dp)
            .fillMaxWidth()
            .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(size = 5.dp))
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(size = 5.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Pódio de desafios",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.W700,
            color = Color(0xFF000000)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MedalCounter("Ouro", summary.gold, Color(0xFFE0A800), Modifier.weight(1f))
            MedalCounter("Prata", summary.silver, Color(0xFF8A94A6), Modifier.weight(1f))
            MedalCounter("Bronze", summary.bronze, Color(0xFFB86E32), Modifier.weight(1f))
            MedalCounter("Part.", summary.participation, Color(0xFF238D25), Modifier.weight(1f))
        }
    }
}

@Composable
fun ChallengeAwardHistorySection(
    summary: ChallengeAwardSummary,
    historyLimit: Int? = 5,
    onAwardClick: (ChallengeAward) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp, top = 10.dp)
            .fillMaxWidth()
            .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(size = 5.dp))
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(size = 5.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Histórico",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.W700,
            color = Color(0xFF000000)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (summary.history.isEmpty()) {
            Text(
                text = "Nenhum desafio finalizado com medalha ainda",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6F6C6C)
            )
        } else {
            val history = historyLimit?.let { summary.history.take(it) } ?: summary.history
            history.forEach { award ->
                ChallengeAwardHistoryRow(
                    award = award,
                    onClick = { onAwardClick(award) }
                )
            }
        }
    }
}

@Composable
private fun MedalCounter(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .background(color = Color.White, shape = RoundedCornerShape(5.dp))
            .border(width = 1.dp, color = color.copy(alpha = 0.45f), shape = RoundedCornerShape(5.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = Inter,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W800,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = Color(0xFF555555)
        )
    }
}

@Composable
private fun ChallengeAwardHistoryRow(
    award: ChallengeAward,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(medalColor(award.medalType), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = award.challengeTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = Color(0xFF202020),
                maxLines = 1
            )
            val finishedDate = ChallengeAwardCalculator.formatFinishedDate(award.finishedAt)
            Text(
                text = listOfNotNull(
                    medalLabel(award.medalType, award.position),
                    finishedDate.takeIf { it.isNotBlank() },
                    awardScoreLabel(award)
                ).joinToString(" - "),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6F6C6C),
                maxLines = 1
            )
        }
    }
}

private fun medalLabel(type: ChallengeMedalType, position: Int?): String {
    return when (type) {
        ChallengeMedalType.GOLD -> "Ouro"
        ChallengeMedalType.SILVER -> "Prata"
        ChallengeMedalType.BRONZE -> "Bronze"
        ChallengeMedalType.PARTICIPATION -> position?.let { "${it}º lugar" } ?: "Participação"
    }
}

private fun medalColor(type: ChallengeMedalType): Color {
    return when (type) {
        ChallengeMedalType.GOLD -> Color(0xFFE0A800)
        ChallengeMedalType.SILVER -> Color(0xFF8A94A6)
        ChallengeMedalType.BRONZE -> Color(0xFFB86E32)
        ChallengeMedalType.PARTICIPATION -> Color(0xFF238D25)
    }
}

private fun awardScoreLabel(award: ChallengeAward): String? {
    if (award.score <= 0.0) return null
    return "${UnitFormatters.compactNumber(award.score)} ${award.unit}"
}

@Composable
fun StatCard(type: String, number: Int, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp)
            .background(color = Color(0xFFEFEFEF), shape = RoundedCornerShape(size = 4.dp))
            .border(width = 1.dp, color = Color(0xFFB0B0B0), shape = RoundedCornerShape(size = 4.dp))
            .clickable { onClick() }
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = type, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = Inter,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W700
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(2.dp)
                .background(color = Color(0x4F000000))
        )
    }
}

@Composable
fun Status(status: String?, onClick: (() -> Unit)? = null) {
    val text = status?.takeIf { it.isNotBlank() }
        ?: if (onClick != null) "Como você está hoje?" else return

    Box(
        Modifier
            .wrapContentWidth()
            .height(31.dp)
            .background(color = Color(0xFFE3E3E3), shape = RoundedCornerShape(size = 5.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
    }
}
