package br.com.zenith.ui.screens.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.components.challenge.ChallengeContent
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.challenge.ChallengeViewModel

@Composable
fun ChallengeScreen(
    navController: NavController,
    initialChallengeId: String? = null
) {
    ZenithTheme {
        val viewModel: ChallengeViewModel = viewModel()
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()

        LaunchedEffect(initialChallengeId) {
            viewModel.fetchChallenges(context, initialChallengeId)
        }

        ChallengeContent(
            uiState = uiState,
            isLoading = isLoading,
            isSaving = isSaving,
            onCreateClick = { navController.navigate("create_challenge") },
            onOpenChallenge = viewModel::openChallenge,
            onBackToChallenges = {
                if (initialChallengeId != null) {
                    if (!navController.popBackStack()) viewModel.returnToChallenges()
                } else {
                    viewModel.returnToChallenges()
                }
            },
            onJoinChallenge = { viewModel.joinChallenge(it, context) },
            onCreateForumPost = { challengeId, content, images ->
                viewModel.createChallengeForumPost(challengeId, content, images, context)
            },
            onCreateForumComment = { challengeId, entryId, content ->
                viewModel.createChallengeForumComment(challengeId, entryId, content, context)
            },
            onSetChallengeClosed = { challengeId, closed ->
                viewModel.setChallengeClosed(challengeId, closed, context)
            },
            onDeleteForumPost = { challengeId, postId ->
                viewModel.deleteChallengeForumPost(challengeId, postId, context)
            },
            onOpenProfile = { profile ->
                if (profile.id == uiState.currentUserId) {
                    navController.navigate("profile")
                } else {
                    navController.navigate("user_profile/${profile.id}")
                }
            }
        )
    }
}
