package com.inkr8
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.inkr8.di.Inkr8Root
import com.inkr8.presentation.ui.ConfigurationScreen
import com.inkr8.presentation.viewmodel.SessionViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    private var session: SessionViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as Inkr8App).graph
        if (graph == null) { setContent { ConfigurationScreen() }; return }
        val viewModel = ViewModelProvider(this, graph.sessionFactory)[SessionViewModel::class.java]
        session = viewModel
        setContent {
            Inkr8Root(graph, viewModel) {
                viewModel.beginLogin()
                lifecycleScope.launch {
                    try { viewModel.login(graph.googleTokenProvider.token(this@MainActivity)) }
                    catch (error: CancellationException) { throw error }
                    catch (error: Exception) { viewModel.authError(error.message ?: "No se pudo iniciar sesión con Google.") }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        session?.let { if (it.state.value.signedIn) it.refresh() }
    }
}
