package org.wordpress.android.fluxc.site

import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS

@RunWith(RobolectricTestRunner::class)
class SiteXMLRPCClientTest {
    private lateinit var mSiteXMLRPCClient: SiteXMLRPCClient
    private lateinit var mDispatcher: Dispatcher
    private lateinit var mMockedQueue: RequestQueue
    private var mMockedResponse = ""
    private var mCountDownLatch: CountDownLatch? = null
    @Before fun setUp() {
        ShadowLog.stream = System.out
        mMockedQueue = Mockito.mock(RequestQueue::class.java)
        mDispatcher = Mockito.mock(Dispatcher::class.java)
        doAnswer { invocation ->
            val request = invocation.arguments[0] as XMLRPCRequest
            try {
                val requestClass = Class.forName(
                        "org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest"
                ) as Class<XMLRPCRequest>
                // Reflection code equivalent to:
                // Object o = request.parseNetworkResponse(data)
                val parseNetworkResponse = requestClass.getDeclaredMethod(
                        "parseNetworkResponse",
                        NetworkResponse::class.java
                )
                parseNetworkResponse.isAccessible = true
                val nr = NetworkResponse(mMockedResponse.toByteArray())
                val o = parseNetworkResponse.invoke(request, nr) as Response<Any>
                // Reflection code equivalent to:
                // request.deliverResponse(o)
                val deliverResponse = requestClass.getDeclaredMethod("deliverResponse", Any::class.java)
                deliverResponse.isAccessible = true
                deliverResponse.invoke(request, o.result)
            } catch (e: Exception) {
                Assert.assertTrue("Unexpected exception: $e", false)
            }
            mCountDownLatch?.countDown()
            null
        }.whenever(mMockedQueue).add<Any>(any())

        mSiteXMLRPCClient = SiteXMLRPCClient(
                mDispatcher, mMockedQueue, Mockito.mock(UserAgent::class.java),
                Mockito.mock(HTTPAuthManager::class.java), XMLRPCRequestBuilder()
        )
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()
    }

    @Test @Throws(Exception::class) fun testFetchSite() {
        val site = SiteUtils.generateSelfHostedNonJPSite()
        mCountDownLatch = CountDownLatch(1)
        mMockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
<methodResponse><params><param><value>
  <struct>
  <member><name>post_thumbnail</name><value><struct>
  <member><name>value</name><value><boolean>1</boolean></value></member>
  </struct></value></member>
  
  <member><name>time_zone</name><value><struct>
  <member><name>value</name><value><string>0</string></value></member>
  </struct></value></member>
  
  <member><name>login_url</name><value><struct>
  <member><name>value</name><value>
  <string>https://taliwutblog.wordpress.com/wp-login.php</string>
  </value></member></struct></value></member>
  
  <member><name>blog_public</name><value><struct>
  <member><name>value</name><value><string>0</string></value></member></struct>
  </value></member>
  
  <member><name>blog_title</name><value><struct>
  <member><name>value</name><value><string>@tal&amp;amp;wut blog</string>
  </value></member></struct></value></member>
  
  <member><name>admin_url</name><value><struct>
  <member><name>readonly</name><value><boolean>1</boolean></value></member>
  <member><name>value</name><value>
  <string>https://taliwutblog.wordpress.com/wp-admin/</string>
  </value></member></struct></value></member>
  
  <member><name>software_version</name><value><struct>
  <member><name>value</name><value><string>4.5.3-20160628</string></value></member>
  </struct></value></member>
  
  <member><name>jetpack_client_id</name><value><struct>
  <member><name>value</name><value><string>false</string></value></member></struct>
  </value></member>
  
  <member><name>home_url</name><value><struct>
  <member><name>value</name><value><string>http://taliwutblog.wordpress.com</string>
  </value></member></struct></value></member>
  </struct>
</value></param></params></methodResponse>"""
        mSiteXMLRPCClient.fetchSite(site)
        Assert.assertTrue(mCountDownLatch?.await(UnitTestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS) == true)
    }

    @Test @Throws(Exception::class) fun testFetchSiteBadResponseFormat() {
        // If wp.getOptions returns a String instead of a Map, make sure we:
        // 1. Don't crash
        // 2. Emit an UPDATE_SITE action with an INVALID_RESPONSE error
        // 3. Report the parse error and its details in an OnUnexpectedError
        val site = SiteUtils.generateSelfHostedNonJPSite()
        mMockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
<methodResponse><params><param><value>
  <string>whoops</string>
</value></param></params></methodResponse>"""
        Mockito.doAnswer { invocation -> // Expect UPDATE_SITE to be dispatched with an INVALID_RESPONSE error
            val action = invocation.getArgument<Action<*>>(0)
            Assert.assertEquals(UPDATE_SITE, action.type)
            val result = action.payload as SiteModel
            Assert.assertTrue(result.isError)
            Assert.assertEquals(INVALID_RESPONSE, result.error.type)
            mCountDownLatch?.countDown()
            null
        }.whenever(mDispatcher).dispatch(any())
        mCountDownLatch = CountDownLatch(2)
        mSiteXMLRPCClient.fetchSite(site)

        Assert.assertTrue(mCountDownLatch?.await(UnitTestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS) == true)
    }

    @Test
    fun testFetchSites() = test {
        mMockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
<methodResponse><params><param><value>
<array><data><value><struct>
<member><name>isAdmin</name><value><boolean>1</boolean></value></member>
<member><name>url</name><value>
<string>http://docbrown.url/</string>
</value></member>
<member><name>blogid</name><value><string>1</string></value></member>
<member><name>blogName</name><value><string>Doc Brown Testing</string></value></member>
<member><name>xmlrpc</name><value>
<string>http://docbrown.url/xmlrpc.php</string>
</value></member></struct></value></data></array>
</value></param></params></methodResponse>"""
        val xmlrpcUrl = "http://docbrown.url/xmlrpc.php"
        val fetchedSites = mSiteXMLRPCClient.fetchSites(xmlrpcUrl, "thedoc", "gr3@tsc0tt")

        assertThat(fetchedSites.sites).isNotEmpty
    }

    @Test
    @Throws(Exception::class)
    fun testFetchSitesResponseNotArray() = test {
        mMockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
<methodResponse><params><param><value>
<string>disaster!</string>
</value></param></params></methodResponse>"""
        val xmlrpcUrl = "http://docbrown.url/xmlrpc.php"

        doAnswer { invocation -> // Expect an OnUnexpectedError to be emitted with a parse error
            val event = invocation.getArgument<OnUnexpectedError>(0)
            Assert.assertEquals(xmlrpcUrl, event.extras[OnUnexpectedError.KEY_URL])
            Assert.assertEquals("disaster!", event.extras[OnUnexpectedError.KEY_RESPONSE])
            Assert.assertEquals(java.lang.ClassCastException::class.java, event.exception.javaClass)
            mCountDownLatch?.countDown()
            null
        }.whenever(mDispatcher).emitChange(any())

        mCountDownLatch = CountDownLatch(2)

        mSiteXMLRPCClient.fetchSites(xmlrpcUrl, "thedoc", "gr3@tsc0tt")

        val result = mSiteXMLRPCClient.fetchSites(xmlrpcUrl, "thedoc", "gr3@tsc0tt")

        assertThat(result.isError).isTrue()
        assertThat(result.error.type).isEqualTo(INVALID_RESPONSE)
        Assert.assertTrue(mCountDownLatch!!.await(UnitTestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }
}
