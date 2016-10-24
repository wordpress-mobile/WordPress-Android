package org.wordpress.android.fluxc.site;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wordpress.android.fluxc.utils.SiteUtils.*;

@RunWith(RobolectricTestRunner.class)
public class SiteXMLRPCClientTest {
    private SiteXMLRPCClient mSiteXMLRPCClient;
    private RequestQueue mMockedQueue;
    private String mMockedResponse = "";
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        mMockedQueue = mock(RequestQueue.class);
        when(mMockedQueue.add(any(Request.class))).thenAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                XMLRPCRequest request = (XMLRPCRequest) invocation.getArguments()[0];
                try {
                    Class<XMLRPCRequest> requestClass = (Class<XMLRPCRequest>)
                                    Class.forName("org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest");
                    // Reflection code equivalent to:
                    // Object o = request.parseNetworkResponse(data)
                    Method parseNetworkResponse = requestClass.getDeclaredMethod("parseNetworkResponse",
                            NetworkResponse.class);
                    parseNetworkResponse.setAccessible(true);
                    NetworkResponse nr = new NetworkResponse(mMockedResponse.getBytes());
                    Response<Object> o = (Response<Object>) parseNetworkResponse.invoke(request, nr);
                    // Reflection code equivalent to:
                    // request.deliverResponse(o)
                    Method deliverResponse = requestClass.getDeclaredMethod("deliverResponse", Object.class);
                    deliverResponse.setAccessible(true);
                    deliverResponse.invoke(request, o.result);
                } catch (Exception e) {
                    assertTrue("Unexpected exception: " + e, false);
                }
                mCountDownLatch.countDown();
                return null;
            }
        });
        mSiteXMLRPCClient = new SiteXMLRPCClient(new Dispatcher(), mMockedQueue,
                mock(AccessToken.class), mock(UserAgent.class),
                mock(HTTPAuthManager.class));

        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testFetchSite() throws Exception {
        SiteModel site = generateDotComSite();
        mCountDownLatch = new CountDownLatch(1);
        mMockedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<methodResponse>\n" +
                "  <params>\n" +
                "    <param>\n" +
                "      <value>\n" +
                "      <struct>\n" +
                "  <member><name>post_thumbnail</name><value><struct>\n" +
                "  <member><name>value</name><value><boolean>1</boolean></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>time_zone</name><value><struct>\n" +
                "  <member><name>value</name><value><string>0</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>login_url</name><value><struct>\n" +
                "  <member><name>value</name><value><string>https://taliwutblog.wordpress.com/wp-login.php</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>blog_public</name><value><struct>\n" +
                "  <member><name>value</name><value><string>0</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>blog_title</name><value><struct>\n" +
                "  <member><name>value</name><value><string>@tal&amp;amp;wut blog</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>admin_url</name><value><struct>\n" +
                "  <member><name>readonly</name><value><boolean>1</boolean></value></member>\n" +
                "  <member><name>value</name><value><string>https://taliwutblog.wordpress.com/wp-admin/</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>software_version</name><value><struct>\n" +
                "  <member><name>value</name><value><string>4.5.3-20160628</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>jetpack_client_id</name><value><struct>\n" +
                "  <member><name>value</name><value><string>false</string></value></member>\n" +
                "</struct></value></member>\n" +
                "  <member><name>home_url</name><value><struct>\n" +
                "  <member><name>value</name><value><string>http://taliwutblog.wordpress.com</string></value></member>\n" +
                "</struct></value></member>\n" +
                "</struct>\n" +
                "      </value>\n" +
                "    </param>\n" +
                "  </params>\n" +
                "</methodResponse>\n";
        mSiteXMLRPCClient.fetchSite(site);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
