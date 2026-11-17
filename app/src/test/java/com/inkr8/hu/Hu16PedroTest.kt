package com.inkr8.hu

import androidx.lifecycle.ViewModelStore
import com.inkr8.domain.model.Home
import com.inkr8.domain.model.Season
import com.inkr8.domain.model.Word
import com.inkr8.domain.repository.HomeRepository
import com.inkr8.domain.usecase.GetHome
import com.inkr8.domain.usecase.GetWordExample
import com.inkr8.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * HU 1.6 · Frase de ejemplo · Responsable: Pedro Iván Pacheco Castro
 * Ejecutar: gradlew :app:testDebugUnitTest --tests "com.inkr8.hu.Hu16PedroTest"
 *
 *  CB  HomeViewModel.showExample (ContentViewModels.kt:24)   V(G) = 6
 *      d1 home?. (¿hay Home cargada?)   d2 ?: return (¿hay día?)   d3 if (exampleLoading) return
 *      d4 catch CancellationException   d5 catch Exception          -> V(G) = 5 + 1 = 6
 *      d1 y d2 dependen del mismo dato (wordDay no admite null): "Home cargada sin día" es infactible.
 *      Base: 5 caminos ejecutables + 1 infactible documentado.
 *  CN  frase de ejemplo del día (backend) -> functions/test/hu/hu1_6_pedro.test.cjs del repositorio del backend
 *  UT  GetWordExample.execute (backend)   -> mismo archivo del backend
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Hu16PedroTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private val home = Home(Word("ink", "Colored fluid", "I spilled ink on the page.", "/ɪŋk/", "noun", "Common"), Season("s2", 0, 1), 100, 20773)
    private class HomeFalso(val home: Home?, val ejemplo: suspend (Long) -> String) : HomeRepository {
        val dias = mutableListOf<Long>()
        override suspend fun getHome(): Home = home ?: throw IOException("sin red")
        override suspend fun getExample(day: Long): String { dias += day; return ejemplo(day) }
    }
    private fun crear(repo: HomeFalso) = HomeViewModel(GetHome(repo), GetWordExample(repo)).also { store.put("vm", it) }
    private val MSG_EJEMPLO = "The example could not be loaded. Please try again"

    @Test fun HU16_CB_C01_camino1_sin_Home_cargada_no_pide_el_ejemplo() = runTest(dispatcher) {
        val repo = HomeFalso(null) { "no debe llamarse" }; val vm = crear(repo); runCurrent()
        vm.showExample(); runCurrent()
        assertTrue(repo.dias.isEmpty()); assertNull(vm.state.value.example); assertFalse(vm.state.value.exampleLoading)
    }
    @Test fun HU16_CB_C02_camino2_si_ya_esta_cargando_ignora_el_segundo_toque() = runTest(dispatcher) {
        val respuesta = CompletableDeferred<String>()
        val repo = HomeFalso(home) { respuesta.await() }; val vm = crear(repo); runCurrent()
        vm.showExample(); runCurrent()
        assertTrue(vm.state.value.exampleLoading)
        vm.showExample(); runCurrent()
        assertEquals(listOf(20773L), repo.dias)
        respuesta.complete("I spilled ink on the page."); runCurrent()
        assertEquals("I spilled ink on the page.", vm.state.value.example)
    }
    @Test fun HU16_CB_C03_camino3_base_muestra_la_frase_del_dia_de_la_Home() = runTest(dispatcher) {
        val repo = HomeFalso(home) { "I spilled ink on the page." }; val vm = crear(repo); runCurrent()
        vm.showExample(); runCurrent()
        assertEquals(listOf(20773L), repo.dias)
        assertEquals("I spilled ink on the page.", vm.state.value.example)
        assertFalse(vm.state.value.exampleLoading); assertNull(vm.state.value.exampleError)
    }
    @Test fun HU16_CB_C04_camino4_error_muestra_el_mensaje_de_HU_1_6() = runTest(dispatcher) {
        val vm = crear(HomeFalso(home) { throw IOException("sin red") }); runCurrent()
        vm.showExample(); runCurrent()
        assertEquals(MSG_EJEMPLO, vm.state.value.exampleError)
        assertNull(vm.state.value.example); assertFalse(vm.state.value.exampleLoading)
    }
    @Test fun HU16_CB_C05_camino5_cancelacion_se_propaga_sin_mostrar_error() = runTest(dispatcher) {
        val vm = crear(HomeFalso(home) { throw CancellationException("cancelado") }); runCurrent()
        vm.showExample(); runCurrent()
        assertNull(vm.state.value.exampleError); assertNull(vm.state.value.example)
    }
}
