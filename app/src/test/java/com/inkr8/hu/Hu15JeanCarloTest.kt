package com.inkr8.hu

import com.inkr8.data.mapper.toHome
import com.inkr8.domain.model.Home
import com.inkr8.domain.model.Season
import com.inkr8.domain.model.Word
import org.junit.Assert.*
import org.junit.Test

/**
 * HU 1.5 · Home / Palabra del momento · Responsable: Jean Carlo de Jesús Rado Delgado
 * Ejecutar: gradlew :app:testDebugUnitTest --tests "com.inkr8.hu.Hu15JeanCarloTest"
 *
 *  CB  dailyWord (backend)      -> functions/test/hu/hu1_5_jeancarlo.test.cjs del repositorio del backend
 *  CN  WireMap.toHome() (DomainMappers.kt:10): convierte la respuesta de getHome en lo que muestra la Home.
 *      Entradas (4 campos, 11 valores): word {text, definition, sentence, phonetic, partOfSpeech, rarity},
 *      season {id, start, end}, rankedCost, wordDay.
 *  UT  GetDailyHome.execute (backend) -> mismo archivo del backend
 *
 * Clases:  word/season: V mapa | NV ausente u otro tipo -> "Respuesta de servidor inválida."
 *          textos: V String | NV ausente u otro tipo -> ""
 *          números: V Long/Int/Double (Double se trunca) | NV ausente u otro tipo -> 0
 */
class Hu15JeanCarloTest {
    private val palabra = mapOf<String, Any?>("text" to "serendipity", "definition" to "A fortunate discovery",
        "sentence" to "Finding it was pure serendipity.", "phonetic" to "/ˌser.ənˈdɪp.ə.ti/", "partOfSpeech" to "noun", "rarity" to "Rare")
    private val temporada = mapOf<String, Any?>("id" to "s2", "start" to 1794182400000L, "end" to 1795392000000L)
    private val respuesta = mapOf<String, Any?>("word" to palabra, "season" to temporada, "rankedCost" to 100L, "wordDay" to 20773L)
    private val esperado = Home(Word("serendipity", "A fortunate discovery", "Finding it was pure serendipity.", "/ˌser.ənˈdɪp.ə.ti/", "noun", "Rare"),
        Season("s2", 1794182400000L, 1795392000000L), 100L, 20773L)

    @Test fun HU15_CN_C01_respuesta_completa_muestra_palabra_metadatos_temporada_costo_y_dia() {
        assertEquals(esperado, respuesta.toHome())
    }
    @Test fun HU15_CN_C02_sin_palabra_la_respuesta_es_invalida() {
        val e = assertThrows(IllegalArgumentException::class.java) { (respuesta - "word").toHome() }
        assertEquals("Respuesta de servidor inválida.", e.message)
    }
    @Test fun HU15_CN_C03_temporada_que_no_es_mapa_es_invalida() {
        val e = assertThrows(IllegalArgumentException::class.java) { (respuesta + ("season" to "s2")).toHome() }
        assertEquals("Respuesta de servidor inválida.", e.message)
    }
    @Test fun HU15_CN_C04_metadatos_ausentes_quedan_vacios_sin_romper_la_Home() {
        val home = (respuesta + ("word" to mapOf("text" to "ink"))).toHome()
        assertEquals(Word("ink", "", "", "", "", ""), home.word)
    }
    @Test fun HU15_CN_C05_metadatos_con_tipo_incorrecto_quedan_vacios() {
        val home = (respuesta + ("word" to palabra.mapValues { 42 })).toHome()
        assertEquals(Word("", "", "", "", "", ""), home.word)
    }
    @Test fun HU15_CN_C06_numeros_Double_e_Int_de_Functions_se_convierten_y_ausentes_son_cero() {
        val home = (respuesta + mapOf("rankedCost" to 100.0, "wordDay" to 20773) + ("season" to temporada + ("start" to 1.7941824E12))).toHome()
        assertEquals(100L, home.rankedCost); assertEquals(20773L, home.wordDay); assertEquals(1794182400000L, home.season.start)
        val sinNumeros = (respuesta - "rankedCost" - "wordDay").toHome()
        assertEquals(0L, sinNumeros.rankedCost); assertEquals(0L, sinNumeros.wordDay)
    }
    @Test fun HU15_CN_C07_cada_uno_de_los_seis_campos_de_la_palabra_llega_a_su_salida() {
        listOf("text", "definition", "sentence", "phonetic", "partOfSpeech", "rarity").forEachIndexed { i, campo ->
            val w = (respuesta + ("word" to palabra + (campo to "cambiado"))).toHome().word
            val salida = listOf(w.text, w.definition, w.sentence, w.phonetic, w.partOfSpeech, w.rarity)
            assertEquals(campo, "cambiado", salida[i])
            assertEquals(campo, 5, salida.count { it != "cambiado" })
        }
    }
}
