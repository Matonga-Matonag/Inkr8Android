package com.inkr8.hu

import androidx.lifecycle.ViewModelStore
import com.inkr8.domain.model.User
import com.inkr8.domain.repository.AuthRepository
import com.inkr8.domain.repository.UserRepository
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.viewmodel.SessionViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * HU 1.4 · Cierre de sesión · Responsable: Víctor Manuel Camarena Paredes
 * Ejecutar: gradlew :app:testDebugUnitTest --tests "com.inkr8.hu.Hu14VictorTest"
 *
 *  CB  authenticated (backend)       -> functions/test/hu/hu1_4_victor.test.cjs del repositorio del backend
 *  CN  acceso con y sin sesión (backend) -> mismo archivo del backend
 *  UT  SessionViewModel.logout (SessionViewModel.kt:48) con repositorios falsos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Hu14VictorTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private val victor = User("u-victor", "Víctor", "v@example.test", 1000, 0, 1, "victor")
    private class AuthFalso(var error: Exception? = null) : AuthRepository {
        val uid = MutableStateFlow<String?>("u-victor"); var cierres = 0
        override fun observeSession(): Flow<String?> = uid
        override suspend fun signInWithGoogle(token: String) { uid.value = "u-victor" }
        override suspend fun signOut() { cierres++; error?.let { throw it }; uid.value = null }
    }
    private class UsuariosFalso(val perfil: User) : UserRepository {
        var inicializaciones = 0; val cambios = MutableSharedFlow<User>(extraBufferCapacity = 4)
        override suspend fun initialize(): User { inicializaciones++; return perfil }
        override fun observeUser(id: String): Flow<User> = cambios
        override suspend fun updateName(name: String) {}
    }
    private fun crear(auth: AuthFalso, users: UsuariosFalso = UsuariosFalso(victor)) =
        SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth)).also { store.put("vm", it) }
    private val MSG_LOGOUT = "Error when logging out. Please try again"

    @Test fun HU14_UT_C01_cerrar_sesion_llama_una_vez_a_signOut_y_vuelve_al_login() = runTest(dispatcher) {
        val auth = AuthFalso(); val vm = crear(auth); runCurrent()
        assertEquals(victor, vm.state.value.user)
        vm.logout(); runCurrent()
        assertEquals(1, auth.cierres)
        assertFalse(vm.state.value.signedIn); assertNull(vm.state.value.user)
        assertFalse(vm.state.value.loading); assertNull(vm.state.value.error)
    }
    @Test fun HU14_UT_C02_si_signOut_falla_muestra_el_mensaje_y_mantiene_la_sesion() = runTest(dispatcher) {
        val vm = crear(AuthFalso(IOException("sin red"))); runCurrent()
        vm.logout(); runCurrent()
        assertEquals(MSG_LOGOUT, vm.state.value.error)
        assertTrue(vm.state.value.signedIn); assertEquals(victor, vm.state.value.user)
    }
    @Test fun HU14_UT_C03_despues_de_cerrar_sesion_no_se_aplican_cambios_tardios_del_perfil() = runTest(dispatcher) {
        val users = UsuariosFalso(victor); val vm = crear(AuthFalso(), users); runCurrent()
        vm.logout(); runCurrent()
        users.cambios.emit(victor.copy(merit = 5000)); runCurrent()
        assertNull(vm.state.value.user)
    }
    @Test fun HU14_UT_C04_una_cancelacion_no_se_muestra_como_error() = runTest(dispatcher) {
        val vm = crear(AuthFalso(CancellationException("cancelado"))); runCurrent()
        vm.logout(); runCurrent()
        assertNull(vm.state.value.error); assertEquals(victor, vm.state.value.user)
    }
    @Test fun HU14_UT_C05_tras_un_fallo_se_puede_reintentar_y_cerrar_sesion() = runTest(dispatcher) {
        val auth = AuthFalso(IOException("sin red")); val vm = crear(auth); runCurrent()
        vm.logout(); runCurrent()
        auth.error = null
        vm.logout(); runCurrent()
        assertEquals(2, auth.cierres); assertFalse(vm.state.value.signedIn); assertNull(vm.state.value.error)
    }
}
