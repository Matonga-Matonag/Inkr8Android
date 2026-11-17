package com.inkr8.hu

import androidx.lifecycle.ViewModelStore
import com.inkr8.data.dto.UserDto
import com.inkr8.data.mapper.toDomain
import com.inkr8.domain.model.User
import com.inkr8.domain.repository.AuthRepository
import com.inkr8.domain.repository.UserRepository
import com.inkr8.domain.usecase.*
import com.inkr8.presentation.viewmodel.SessionViewModel
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
 * HU 1.3 · Sesión persistente · Responsable: Marco Antonio Bocanegra Carrión
 * Ejecutar: gradlew :app:testDebugUnitTest --tests "com.inkr8.hu.Hu13MarcoTest"
 *
 *  CB  mapUser (backend)                -> functions/test/hu/hu1_3_marco.test.cjs del repositorio del backend
 *  CN  UserDto.read(id, map).toDomain() -> perfil que la app reconstruye al reabrirse con la sesión guardada
 *      (FirebaseUserRepository.initialize y observeUser). Entradas (7): id, name, email, merit, rating, league, username.
 *  UT  SessionViewModel (init + refresh): restauración de la sesión sin volver a iniciar sesión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Hu13MarcoTest {
    // ---------- CAJA NEGRA: UserDto.read(id, map).toDomain() ----------
    // name:     V texto | NV ausente u otro tipo -> ""
    // email:    V texto | NV ausente, null u otro tipo -> null
    // merit, rating: V número (Long de Firestore, Int o Double de Functions; Double se trunca) | NV ausente u otro tipo -> 0
    // league:   V número -> Int | NV ausente -> 0
    // username: V texto | NV ausente, null u otro tipo -> null
    private val guardado = mapOf<String, Any?>("name" to "Marco", "email" to "marco@example.test", "merit" to 1250L,
        "rating" to 64L, "league" to 3L, "username" to "marco")
    private fun leer(map: Map<String, Any?>) = UserDto.read("u-marco", map).toDomain()

    @Test fun HU13_CN_C01_perfil_completo_de_Firestore_se_restaura_igual() {
        assertEquals(User("u-marco", "Marco", "marco@example.test", 1250, 64, 3, "marco"), leer(guardado))
    }
    @Test fun HU13_CN_C02_documento_sin_campos_usa_los_valores_por_defecto() {
        assertEquals(User("u-marco", "", null, 0, 0, 0, null), leer(emptyMap()))
    }
    @Test fun HU13_CN_C03_tipos_incorrectos_se_tratan_como_ausentes() {
        val erroneo = mapOf<String, Any?>("name" to 42, "email" to 7, "merit" to "1250", "rating" to true, "league" to "3", "username" to 99)
        assertEquals(User("u-marco", "", null, 0, 0, 0, null), leer(erroneo))
    }
    @Test fun HU13_CN_C04_numeros_Int_y_Double_de_Functions_se_convierten_y_truncan() {
        val user = leer(guardado + mapOf("merit" to 1250.9, "rating" to 64, "league" to 3.0))
        assertEquals(1250L, user.merit); assertEquals(64L, user.rating); assertEquals(3, user.league)
    }
    @Test fun HU13_CN_C05_email_y_username_null_explicitos_quedan_null() {
        val user = leer(guardado + mapOf("email" to null, "username" to null))
        assertNull(user.email); assertNull(user.username); assertEquals("Marco", user.name)
    }
    @Test fun HU13_CN_C06_cada_campo_recibido_solo_afecta_a_su_salida() {
        val cambios = mapOf<String, Any?>("name" to "Otro", "email" to "otro@example.test", "merit" to 1L, "rating" to 2L, "league" to 6L, "username" to "otro")
        val esperados = listOf<Any?>("Otro", "otro@example.test", 1L, 2L, 6, "otro")
        cambios.entries.forEachIndexed { i, (campo, valor) ->
            val u = leer(guardado + (campo to valor))
            val salida = listOf<Any?>(u.name, u.email, u.merit, u.rating, u.league, u.username)
            val base = listOf<Any?>("Marco", "marco@example.test", 1250L, 64L, 3, "marco")
            salida.forEachIndexed { j, v -> assertEquals("campo $campo, salida $j", if (i == j) esperados[j] else base[j], v) }
        }
    }

    // ---------- PRUEBA UNITARIA: SessionViewModel (restauración de sesión) ----------
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private val marco = User("u-marco", "Marco", "marco@example.test", 1250, 64, 3, "marco")
    private class AuthFalso(uidGuardado: String?) : AuthRepository {
        val uid = MutableStateFlow(uidGuardado); var inicios = 0
        override fun observeSession(): Flow<String?> = uid
        override suspend fun signInWithGoogle(token: String) { inicios++ }
        override suspend fun signOut() { uid.value = null }
    }
    private class UsuariosFalso(var perfil: User, var fallos: Int = 0) : UserRepository {
        var inicializaciones = 0; val cambios = MutableSharedFlow<User>(extraBufferCapacity = 4)
        override suspend fun initialize(): User { inicializaciones++; if (fallos-- > 0) throw IOException("sin red"); return perfil }
        override fun observeUser(id: String): Flow<User> = cambios
        override suspend fun updateName(name: String) {}
    }
    private fun crear(auth: AuthFalso, users: UsuariosFalso) =
        SessionViewModel(ObserveSession(auth), InitializeUser(users), ObserveUser(users), SignIn(auth), SignOut(auth)).also { store.put("vm", it) }
    private val MSG_PROGRESO = "We were unable to retrieve your progress information. Please try again"

    @Test fun HU13_UT_C01_sesion_guardada_entra_directo_sin_volver_a_iniciar_sesion() = runTest(dispatcher) {
        val auth = AuthFalso("u-marco"); val users = UsuariosFalso(marco)
        val vm = crear(auth, users); runCurrent()
        assertTrue(vm.state.value.signedIn); assertFalse(vm.state.value.loading)
        assertEquals(marco, vm.state.value.user)
        assertEquals(0, auth.inicios); assertEquals(1, users.inicializaciones)
    }
    @Test fun HU13_UT_C02_sin_sesion_guardada_muestra_login_y_no_consulta_el_perfil() = runTest(dispatcher) {
        val users = UsuariosFalso(marco)
        val vm = crear(AuthFalso(null), users); runCurrent()
        assertFalse(vm.state.value.signedIn); assertFalse(vm.state.value.loading); assertNull(vm.state.value.user)
        assertEquals(0, users.inicializaciones)
    }
    @Test fun HU13_UT_C03_si_falla_la_carga_del_perfil_muestra_el_mensaje_y_conserva_la_sesion() = runTest(dispatcher) {
        val vm = crear(AuthFalso("u-marco"), UsuariosFalso(marco, fallos = 1)); runCurrent()
        assertTrue(vm.state.value.signedIn); assertNull(vm.state.value.user)
        assertEquals(MSG_PROGRESO, vm.state.value.error)
    }
    @Test fun HU13_UT_C04_al_reanudar_la_app_refresh_reintenta_y_limpia_el_error() = runTest(dispatcher) {
        val users = UsuariosFalso(marco, fallos = 1)
        val vm = crear(AuthFalso("u-marco"), users); runCurrent()
        vm.refresh(); runCurrent()
        assertEquals(marco, vm.state.value.user); assertNull(vm.state.value.error)
        assertEquals(2, users.inicializaciones)
    }
    @Test fun HU13_UT_C05_cambios_del_perfil_en_Firestore_se_reflejan_en_la_sesion_restaurada() = runTest(dispatcher) {
        val users = UsuariosFalso(marco)
        val vm = crear(AuthFalso("u-marco"), users); runCurrent()
        users.cambios.emit(marco.copy(merit = 1400)); runCurrent()
        assertEquals(1400L, vm.state.value.user?.merit)
    }
}
