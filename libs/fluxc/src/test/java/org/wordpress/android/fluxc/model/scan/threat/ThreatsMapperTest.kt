package org.wordpress.android.fluxc.model.scan.threat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable.FixType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat.Fixable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class ThreatsMapperTest {
    private lateinit var mapper: ThreatMapper

    @Before
    fun setUp() {
        mapper = ThreatMapper()
    }

    @Test
    fun `maps generic threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_GENERIC_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertTrue(model is GenericThreatModel)
        model.baseThreatModel.apply {
            assertEquals(id, threat.id)
            assertEquals(signature, threat.signature)
            assertEquals(description, threat.description)
            assertEquals(status, ThreatStatus.fromValue(threat.status))
            assertEquals(firstDetected, threat.firstDetected)
            assertEquals(fixedOn, threat.fixedOn)
            assertNull(fixable)
        }
    }

    @Test
    fun `maps file threat with context as json object correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(
            javaClass,
            THREAT_FILE_WITH_CONTEXT_AS_JSON_OBJECT_JSON
        )
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertTrue(model is FileThreatModel)

        model.apply {
            assertNotNull(context)
            assertNotNull(context.lines)
            assertTrue(requireNotNull(context.lines).isNotEmpty())
            assertEquals(context.lines.size, 3)

            context.lines[0].apply {
                assertEquals(lineNumber, 3)
                assertEquals(contents, "echo <<<HTML")
            }

            val highlights = context.lines[1].highlights?.get(0)
            highlights?.let {
                val (startIndex, endIndex) = it
                assertEquals(startIndex, 0)
                assertEquals(endIndex, 68)
            }
        }
    }

    @Test
    fun `maps file threat with context received as string correctly`() {
        val threatContextJsonString = UnitTestUtils.getStringFromResourceFile(
            javaClass,
            THREAT_FILE_WITH_CONTEXT_AS_STRING_JSON
        )
        val threat = getThreatFromJsonString(threatContextJsonString)

        val model = mapper.map(threat)

        assertTrue(model is FileThreatModel)

        model.apply {
            assertNotNull(context)
            assertTrue(requireNotNull(context.lines).isEmpty())
        }
    }

    @Test
    fun `maps core file modification threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_CORE_FILE_MODIFICATION_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertTrue(model is CoreFileModificationThreatModel)

        model.apply {
            assertEquals(diff, threat.diff)
            assertEquals(fileName, threat.fileName)
        }
    }

    @Test
    fun `maps database threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_DATABASE_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertTrue(model is DatabaseThreatModel)
        assertEquals(model.rows, threat.rows)
    }

    @Test
    fun `maps vulnerable extension threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_VULNERABLE_EXTENSION_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertTrue(model is VulnerableExtensionThreatModel)

        model.apply {
            assertEquals(extension.type, ExtensionType.fromValue(threat.extension?.type))
            assertEquals(extension.slug, threat.extension?.slug)
            assertEquals(extension.name, threat.extension?.name)
            assertEquals(extension.version, threat.extension?.version)
            assertEquals(extension.isPremium, threat.extension?.isPremium)
        }
    }

    @Test
    fun `maps fixable threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_FIXABLE_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertNotNull(model.baseThreatModel.fixable)
        assertEquals(model.baseThreatModel.fixable?.fixer, FixType.fromValue(threat.fixable?.fixer))
    }

    @Test
    fun `maps fixable threat correctly for unknown fix type`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_FIXABLE_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat.copy(fixable = Fixable(fixer = "rollback", file = null, target = null)))

        assertEquals(model.baseThreatModel.fixable?.fixer, FixType.UNKNOWN)
    }

    @Test
    fun `maps not fixable threat correctly`() {
        val threatJsonString = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_NOT_FIXABLE_JSON)
        val threat = getThreatFromJsonString(threatJsonString)

        val model = mapper.map(threat)

        assertNull(model.baseThreatModel.fixable)
    }

    private fun getThreatFromJsonString(json: String): Threat {
        val responseType = object : TypeToken<Threat>() {}.type
        return Gson().fromJson(json, responseType) as Threat
    }

    companion object {
        private const val THREAT_GENERIC_JSON = "wp/jetpack/scan/threat/threat-generic.json"
        private const val THREAT_CORE_FILE_MODIFICATION_JSON =
            "wp/jetpack/scan/threat/threat-core-file-modification.json"
        private const val THREAT_DATABASE_JSON = "wp/jetpack/scan/threat/threat-database.json"
        private const val THREAT_FILE_WITH_CONTEXT_AS_JSON_OBJECT_JSON =
            "wp/jetpack/scan/threat/threat-file-with-context-as-json-object.json"
        private const val THREAT_FILE_WITH_CONTEXT_AS_STRING_JSON =
            "wp/jetpack/scan/threat/threat-file-with-context-as-string.json"
        private const val THREAT_VULNERABLE_EXTENSION_JSON = "wp/jetpack/scan/threat/threat-vulnerable-extension.json"
        private const val THREAT_FIXABLE_JSON = "wp/jetpack/scan/threat/threat-fixable.json"
        private const val THREAT_NOT_FIXABLE_JSON = "wp/jetpack/scan/threat/threat-not-fixable.json"
    }
}
