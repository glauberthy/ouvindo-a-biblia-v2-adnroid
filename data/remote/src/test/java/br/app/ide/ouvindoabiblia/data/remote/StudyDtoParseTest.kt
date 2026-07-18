package br.app.ide.ouvindoabiblia.data.remote

import br.app.ide.ouvindoabiblia.data.remote.dto.StudyResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ISSUE 9.A — o campo `description` por aula (audios[]) é opcional:
 * JSON novo (com o campo) e antigo (sem) precisam ambos parsear.
 */
class StudyDtoParseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `audio com description parseia e expoe o texto`() {
        val payload = """
            {
              "meta": { "version": "0.0.21" },
              "estudos": [
                {
                  "id": 1,
                  "title": "Estudos Expositivos em Apocalipse",
                  "description": "Série de estudos.",
                  "image_url": "https://x/cover.webp",
                  "audios": [
                    {
                      "id": 1,
                      "title": "Características literárias do Apocalipse",
                      "url": "https://x/01.m4a",
                      "duration": 5885,
                      "description": "Aula introdutória que estabelece os fundamentos."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<StudyResponseDto>(payload)
        val audio = parsed.estudos.single().audios.single()

        assertEquals("Aula introdutória que estabelece os fundamentos.", audio.description)
        assertEquals(5885L, audio.duration)
    }

    @Test
    fun `audio sem description parseia com null (JSON antigo)`() {
        val payload = """
            {
              "meta": { "version": "0.0.20" },
              "estudos": [
                {
                  "id": 1,
                  "title": "Estudo",
                  "description": "Série.",
                  "image_url": "https://x/cover.webp",
                  "audios": [
                    { "id": 1, "title": "Aula 1", "url": "https://x/01.m4a" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<StudyResponseDto>(payload)
        val audio = parsed.estudos.single().audios.single()

        assertNull(audio.description)
        assertNull(audio.duration)
    }
}
