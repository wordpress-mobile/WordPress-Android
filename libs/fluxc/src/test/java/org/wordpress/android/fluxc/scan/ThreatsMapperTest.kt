package org.wordpress.android.fluxc.scan

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class ThreatsMapperTest {
    private lateinit var mapper: ThreatMapper

    @Before
    fun setUp() {
        mapper = ThreatMapper()
    }

    @Test
    fun `maps threat fields correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertEquals(model.id, threat.id)
        assertEquals(model.signature, threat.signature)
        assertEquals(model.description, threat.description)
        assertEquals(model.status, threat.status)
        assertEquals(model.firstDetected, threat.firstDetected)
        assertEquals(model.fixedOn, threat.fixedOn)
        model.fixable?.apply {
            assertEquals(fixer, threat.fixable?.fixer)
            assertEquals(target, threat.fixable?.target)
        }
        model.extension?.apply {
            assertEquals(type, threat.extension?.type)
            assertEquals(slug, threat.extension?.slug)
            assertEquals(name, threat.extension?.name)
            assertEquals(version, threat.extension?.version)
            assertEquals(isPremium, threat.extension?.isPremium)
        }
        assertEquals(model.fileName, threat.fileName)
        assertEquals(model.rows, threat.rows)
        assertEquals(model.diff, threat.diff)
    }

    @Test
    fun `maps threat context received as json correctly`() {
        val threatContextJsonString = UnitTestUtils.getStringFromResourceFile(
            javaClass,
            THREAT_CONTEXT_AS_JSON_OBJECT_JSON
        )
        val threat = getThreatFromJsonString(threatContextJsonString)

        val model = mapper.map(threat)

        assertNotNull(model.context)
        assertNotNull(model.context?.lines)
        assertTrue(requireNotNull(model.context?.lines).isNotEmpty())

        assertEquals(model.context?.lines?.size, 3)

        model.context?.lines?.get(0)?.apply {
            assertEquals(lineNumber, 3)
            assertEquals(contents, "echo <<<HTML")
        }

        val highlights = model.context?.lines?.get(1)?.highlights?.get(0)
        highlights?.let {
            val (startIndex, endIndex) = it
            assertEquals(startIndex, 0)
            assertEquals(endIndex, 68)
        }
    }

    @Test
    fun `maps threat context received as string correctly`() {
        val threatContextJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_CONTEXT_AS_STRING_JSON)
        val threat = getThreatFromJsonString(threatContextJsonString)

        val model = mapper.map(threat)

        assertNotNull(model.context)
        assertTrue(requireNotNull(model.context?.lines).isEmpty())
    }

    private fun getThreatFromJsonString(json: String): Threat {
        val responseType = object : TypeToken<Threat>() {}.type
        return Gson().fromJson(json, responseType) as Threat
    }

    companion object {
        private const val THREAT_JSON = "wp/jetpack/scan/threat/threat.json"
        private const val THREAT_CONTEXT_AS_JSON_OBJECT_JSON =
            "wp/jetpack/scan/threat/threat_context_as_json_object.json"
        private const val THREAT_CONTEXT_AS_STRING_JSON = "wp/jetpack/scan/threat/threat_context_as_string.json"
    }
}
