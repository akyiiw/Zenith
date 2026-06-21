package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.common.ZenithFilterBar
import br.com.zenith.ui.components.common.ZenithFilterOption
import br.com.zenith.ui.components.profile.RecentActivityCard
import br.com.zenith.ui.components.profile.groupNamesByActivityId
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.social.PublicProfileViewModel

private enum class VerificationFilter(val label: String) {
    All("Todas"),
    Verified("Verificadas"),
    Manual("Manuais")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileActivitiesScreen(
    navController: NavController,
    userId: String
) {
    ZenithTheme {
        val context = LocalContext.current
        val viewModel: PublicProfileViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var exerciseFilter by remember(userId) { mutableStateOf<String?>(null) }
        var groupFilter by remember(userId) { mutableStateOf<String?>(null) }
        var verificationFilter by remember(userId) { mutableStateOf(VerificationFilter.All) }

        LaunchedEffect(userId) {
            viewModel.fetchProfile(userId, context)
        }

        val exercises = remember(uiState.atividades) {
            uiState.atividades
                .map { it.exercicio?.nome ?: "Atividade" }
                .distinct()
                .sorted()
        }
        val groupsById = remember(uiState.activityGroups) {
            uiState.activityGroups.associateBy { it.id }
        }
        val groupNames = remember(uiState.activityGroups) {
            uiState.activityGroups.map { it.name }.distinct().sorted()
        }
        val activityIdsByGroupName = remember(uiState.activityGroupItems, groupsById) {
            uiState.activityGroupItems
                .groupBy { item -> groupsById[item.groupId]?.name }
                .filterKeys { it != null }
                .mapKeys { it.key.orEmpty() }
                .mapValues { entry -> entry.value.map { it.activityId }.toSet() }
        }
        val activityGroupNamesByActivityId = remember(uiState.activityGroups, uiState.activityGroupItems) {
            groupNamesByActivityId(uiState.activityGroups, uiState.activityGroupItems)
        }
        val filteredActivities = remember(
            uiState.atividades,
            exerciseFilter,
            groupFilter,
            verificationFilter,
            activityIdsByGroupName
        ) {
            uiState.atividades
                .filter { activity ->
                    exerciseFilter == null || (activity.exercicio?.nome ?: "Atividade") == exerciseFilter
                }
                .filter { activity ->
                    groupFilter == null || activity.id in activityIdsByGroupName[groupFilter].orEmpty()
                }
                .filter { activity ->
                    when (verificationFilter) {
                        VerificationFilter.All -> true
                        VerificationFilter.Verified -> activity.verificada
                        VerificationFilter.Manual -> !activity.verificada
                    }
                }
                .sortedByDescending { it.realizadaEm ?: it.criadaEm.orEmpty() }
        }

        Scaffold(containerColor = Color.White) { padding ->
            if (isLoading) {
                CenteredZenithLoading(contentPadding = padding)
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                contentPadding = PaddingValues(bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                        Text(
                            text = "Atividades",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (!uiState.canViewActivities) {
                    item {
                        EmptyActivitiesMessage("Perfil privado")
                    }
                    return@LazyColumn
                }

                item {
                    ActivityFilterRow(
                        exercises = exercises,
                        groups = groupNames,
                        exerciseFilter = exerciseFilter,
                        groupFilter = groupFilter,
                        verificationFilter = verificationFilter,
                        onExerciseFilterChange = { exerciseFilter = it },
                        onGroupFilterChange = { groupFilter = it },
                        onVerificationFilterChange = { verificationFilter = it }
                    )
                }

                if (filteredActivities.isEmpty()) {
                    item { EmptyActivitiesMessage("Nenhuma atividade encontrada") }
                } else {
                    items(filteredActivities, key = { it.id }) { activity ->
                        RecentActivityCard(
                            atividade = activity,
                            groupNames = activityGroupNamesByActivityId[activity.id].orEmpty(),
                            acceptedMentions = uiState.acceptedMentionsByActivityId[activity.id].orEmpty(),
                            onClick = { navController.navigate("activity_detail/${activity.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFilterRow(
    exercises: List<String>,
    groups: List<String>,
    exerciseFilter: String?,
    groupFilter: String?,
    verificationFilter: VerificationFilter,
    onExerciseFilterChange: (String?) -> Unit,
    onGroupFilterChange: (String?) -> Unit,
    onVerificationFilterChange: (VerificationFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ZenithFilterBar(
                options = VerificationFilter.entries.map { ZenithFilterOption(it, it.label) },
                selectedValue = verificationFilter,
                onSelected = onVerificationFilterChange
            )
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ZenithFilterBar(
                options = listOf(ZenithFilterOption<String?>(null, "Todos")) +
                    exercises.map { ZenithFilterOption<String?>(it, it) },
                selectedValue = exerciseFilter,
                onSelected = onExerciseFilterChange
            )
        }
        if (groups.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                ZenithFilterBar(
                    options = listOf(ZenithFilterOption<String?>(null, "Todos os grupos")) +
                        groups.map { ZenithFilterOption<String?>(it, it) },
                    selectedValue = groupFilter,
                    onSelected = onGroupFilterChange
                )
            }
        }
    }
}

@Composable
private fun EmptyActivitiesMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6F6C6C)
        ),
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
    )
}
