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
import org.wordpress.android.fluxc.UnitTestUtils;
import org.wordpress.android.fluxc.action.SiteAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.model.SiteHomepageSettings;
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Page;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite;
import static org.wordpress.android.fluxc.site.SiteXMLRPCFixturesKt.SITE_OPTIONS;

@RunWith(RobolectricTestRunner.class)
public class SiteXMLRPCClientTest {
    private SiteXMLRPCClient mSiteXMLRPCClient;
    private Dispatcher mDispatcher;
    private RequestQueue mMockedQueue;
    private String mMockedResponse = "";
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        mMockedQueue = mock(RequestQueue.class);
        mDispatcher = mock(Dispatcher.class);
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
        mSiteXMLRPCClient = new SiteXMLRPCClient(mDispatcher, mMockedQueue, mock(UserAgent.class),
                mock(HTTPAuthManager.class));

        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testFetchSite() throws Exception {
        SiteModel site = generateSelfHostedNonJPSite();
        mCountDownLatch = new CountDownLatch(1);
        mMockedResponse = SITE_OPTIONS;
        mSiteXMLRPCClient.fetchSite(site);
        assertEquals(site.getShowOnFront(), "page");
        assertEquals(site.getPageForPosts(), 2L);
        assertEquals(site.getPageOnFront(), 1L);
        assertTrue(mCountDownLatch.await(UnitTestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUpdateSiteHomepage() throws Exception {
        SiteModel site = generateSelfHostedNonJPSite();
        mCountDownLatch = new CountDownLatch(1);
        mMockedResponse = SITE_OPTIONS;
        Page homepageSettings = new Page(2L, 1L);
        final AtomicBoolean success = new AtomicBoolean(false);
        mSiteXMLRPCClient.updateSiteHomepage(site, homepageSettings, new Function1<SiteModel, Unit>() {
            @Override public Unit invoke(SiteModel siteModel) {
                success.set(true);
                return null;
            }
        }, null);
        assertEquals(site.getShowOnFront(), "page");
        assertEquals(site.getPageForPosts(), 2L);
        assertEquals(site.getPageOnFront(), 1L);
        assertTrue(success.get());
        assertTrue(mCountDownLatch.await(UnitTestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSiteBadResponseFormat() throws Exception {
        // If wp.getOptions returns a String instead of a Map, make sure we:
        // 1. Don't crash
        // 2. Emit an UPDATE_SITE action with an INVALID_RESPONSE error
        // 3. Report the parse error and its details in an OnUnexpectedError
        final SiteModel site = generateSelfHostedNonJPSite();
        mMockedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<methodResponse><params><param><value>\n"
                + "  <string>whoops</string>\n"
                + "</value></param></params></methodResponse>";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Expect an OnUnexpectedError to be emitted with a parse error
                OnUnexpectedError event = invocation.getArgument(0);
                assertEquals(site.getXmlRpcUrl(), event.extras.get(OnUnexpectedError.KEY_URL));
                assertEquals("whoops", event.extras.get(OnUnexpectedError.KEY_RESPONSE));
                assertEquals(ClassCastException.class, event.exception.getClass());

                mCountDownLatch.countDown();
                return null;
            }
        }).when(mDispatcher).emitChange(any(Object.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Expect UPDATE_SITE to be dispatched with an INVALID_RESPONSE error
                Action action = invocation.getArgument(0);
                assertEquals(SiteAction.UPDATE_SITE, action.getType());

                SiteModel result = (SiteModel) action.getPayload();
                assertTrue(result.isError());
                assertEquals(GenericErrorType.INVALID_RESPONSE, result.error.type);

                mCountDownLatch.countDown();
                return null;
            }
        }).when(mDispatcher).dispatch(any(Action.class));

        mCountDownLatch = new CountDownLatch(3);
        mSiteXMLRPCClient.fetchSite(site);
        assertTrue(mCountDownLatch.await(UnitTestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSites() throws Exception {
        mMockedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<methodResponse><params><param><value>\n"
                + "<array><data><value><struct>\n"
                + "<member><name>isAdmin</name><value><boolean>1</boolean></value></member>\n"
                + "<member><name>url</name><value>\n"
                + "<string>http://docbrown.url/</string>\n"
                + "</value></member>\n"
                + "<member><name>blogid</name><value><string>1</string></value></member>\n"
                + "<member><name>blogName</name><value><string>Doc Brown Testing</string></value></member>\n"
                + "<member><name>xmlrpc</name><value>\n"
                + "<string>http://docbrown.url/xmlrpc.php</string>\n"
                + "</value></member></struct></value></data></array>\n"
                + "</value></param></params></methodResponse>";
        final String xmlrpcUrl = "http://docbrown.url/xmlrpc.php";

        mCountDownLatch = new CountDownLatch(1);
        mSiteXMLRPCClient.fetchSites(xmlrpcUrl, "thedoc", "gr3@tsc0tt");
        assertTrue(mCountDownLatch.await(UnitTestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSitesResponseNotArray() throws Exception {
        mMockedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<methodResponse><params><param><value>\n"
                + "<string>disaster!</string>\n"
                + "</value></param></params></methodResponse>";
        final String xmlrpcUrl = "http://docbrown.url/xmlrpc.php";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Expect an OnUnexpectedError to be emitted with a parse error
                OnUnexpectedError event = invocation.getArgument(0);
                assertEquals(xmlrpcUrl, event.extras.get(OnUnexpectedError.KEY_URL));
                assertEquals("disaster!", event.extras.get(OnUnexpectedError.KEY_RESPONSE));
                assertEquals(ClassCastException.class, event.exception.getClass());

                mCountDownLatch.countDown();
                return null;
            }
        }).when(mDispatcher).emitChange(any(Object.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Expect UPDATE_SITES to be dispatched with an INVALID_RESPONSE error
                Action action = invocation.getArgument(0);
                assertEquals(SiteAction.FETCHED_SITES_XML_RPC, action.getType());

                SitesModel result = (SitesModel) action.getPayload();
                assertTrue(result.isError());
                assertEquals(GenericErrorType.INVALID_RESPONSE, result.error.type);

                mCountDownLatch.countDown();
                return null;
            }
        }).when(mDispatcher).dispatch(any(Action.class));

        mCountDownLatch = new CountDownLatch(3);
        mSiteXMLRPCClient.fetchSites(xmlrpcUrl, "thedoc", "gr3@tsc0tt");
        assertTrue(mCountDownLatch.await(UnitTestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
