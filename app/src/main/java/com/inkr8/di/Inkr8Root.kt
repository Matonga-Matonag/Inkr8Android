package com.inkr8.di

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inkr8.domain.model.PlayMode
import com.inkr8.presentation.ui.*
import com.inkr8.presentation.viewmodel.*

@Composable
fun Inkr8Root(graph: AppGraph, session: SessionViewModel, onGoogleLogin: () -> Unit) {
    val state by session.state.collectAsStateWithLifecycle()
    val user = state.user
    Inkr8Theme {
        if (user == null) LoginScreen(state, onGoogleLogin, session::refresh, session::logout)
        else key(user.id) {
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                val home: HomeViewModel = viewModel(factory = graph.homeFactory)
                val profile: ProfileViewModel = viewModel(factory = graph.profileFactory)
                val ranking: RankingViewModel = viewModel(factory = graph.rankingFactory)
                val practice: GameViewModel = viewModel(key = "practice", factory = graph.gameFactory(user.id, PlayMode.PRACTICE))
                val ranked: GameViewModel = viewModel(key = "ranked", factory = graph.gameFactory(user.id, PlayMode.RANKED))
                MainScreen(user, state.error, session::refresh, session::logout, home, profile, ranking, practice, ranked)
            }
        }
    }
}
