package org.wordpress.android.fluxc.scan

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ThreatsMapperTest {
    @Mock lateinit var site: SiteModel
    private lateinit var mapper: ThreatMapper

    @Before
    fun setUp() {
        mapper = ThreatMapper()
    }

    @Test
    fun `maps threat fields correctly`() {
        val threatson = UnitTestUtils.getStringFromResourceFile(javaClass, THREAT_JSON)
        val threat = getThreatFromJsonString(threatson)

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
    }

    private fun getThreatFromJsonString(json: String): Threat {
        val responseType = object : TypeToken<Threat>() {}.type
        return Gson().fromJson(json, responseType) as Threat
    }

    companion object {
        private const val THREAT_JSON = "wp/jetpack/threat/threat.json"
    }
}
