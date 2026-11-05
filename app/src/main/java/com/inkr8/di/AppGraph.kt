package com.inkr8.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.inkr8.data.repository.*
import com.inkr8.data.source.*
import com.inkr8.domain.model.PlayMode
import com.inkr8.domain.repository.*
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.auth.GoogleTokenProvider
import com.inkr8.presentation.viewmodel.*

class AppGraph(
    context: Context,
    authRepository: AuthRepository? = null,
    userRepository: UserRepository? = null,
    gameRepository: GameRepository? = null,
    homeRepository: HomeRepository? = null,
    seasonRepository: SeasonRepository? = null,
    draftRepository: DraftRepository? = null,
    tokenProvider: GoogleTokenProvider? = null,
    progressRepository: ProgressRepository? = null
) {
    private val google by lazy { GoogleSignInDataSource(context) }
    val googleTokenProvider: GoogleTokenProvider by lazy {
        tokenProvider ?: object : GoogleTokenProvider {
            override suspend fun token(activityContext: Context): String = google.token(activityContext)
        }
    }
    private val authSource by lazy { FirebaseAuthDataSource(FirebaseAuth.getInstance()) }
    private val functions by lazy { FunctionsDataSource(FirebaseFunctions.getInstance("us-central1")) }
    private val firestore by lazy { FirestoreDataSource(FirebaseFirestore.getInstance()) }
    private val auth: AuthRepository by lazy { authRepository ?: FirebaseAuthRepository(authSource, google) }
    private val users: UserRepository by lazy { userRepository ?: FirebaseUserRepository(functions, firestore) }
    private val games: GameRepository by lazy { gameRepository ?: FirebaseGameRepository(functions, firestore, authSource) }
    private val home: HomeRepository by lazy { homeRepository ?: FirebaseHomeRepository(functions) }
    private val seasons: SeasonRepository by lazy { seasonRepository ?: FirebaseSeasonRepository(functions) }
    private val progress: ProgressRepository by lazy { progressRepository ?: FirebaseProgressRepository(functions) }
    private val drafts: DraftRepository by lazy {
        draftRepository ?: PreferencesDraftRepository(context.getSharedPreferences("writing_drafts", Context.MODE_PRIVATE))
    }
    private val loadDraft by lazy { LoadDraft(drafts) }
    private val saveDraft by lazy { SaveDraft(drafts) }
    private val clearDraft by lazy { ClearDraft(drafts) }
    private val findDraft by lazy { FindDraft(drafts) }
    val sessionFactory = factory { SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth)) }
    val homeFactory = factory { HomeViewModel(GetHome(home), GetWordExample(home)) }
    val rankingFactory = factory { RankingViewModel(GetSeasonRanking(seasons)) }
    val profileFactory = factory { ProfileViewModel(UpdateName(users), GetProgress(progress), GetSeasonHistory(seasons)) }
    fun gameFactory(userId: String, mode: PlayMode) = factory {
        GameViewModel(userId, mode, StartGame(games), SubmitWriting(games), ObserveGame(games), GetRecentGames(games), RetryEvaluation(games),
            loadDraft, saveDraft, clearDraft, findDraft)
    }
    private fun <T : ViewModel> factory(create: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <R : ViewModel> create(modelClass: Class<R>): R = create() as R
    }
}
