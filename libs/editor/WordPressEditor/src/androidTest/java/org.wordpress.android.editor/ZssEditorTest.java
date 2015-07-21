package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.JavascriptInterface;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the <code>ZSSEditor</code> inside an <code>EditorWebViewAbstract</code>, with no UI.
 */
public class ZssEditorTest extends ActivityInstrumentationTestCase2<MockActivity> {
    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";

    private Instrumentation mInstrumentation;
    private EditorWebViewAbstract mWebView;

    private CountDownLatch mSetUpLatch;

    private TestMethod mTestMethod;
    private CountDownLatch mCallbackLatch;
    private CountDownLatch mDomLoadedCallbackLatch;
    private Set<String> mCallbackSet;

    private enum TestMethod {
        INIT
    }

    public ZssEditorTest() {
        super(MockActivity.class);
    }

    @SuppressLint("AddJavascriptInterface")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        Activity activity = getActivity();
        mSetUpLatch = new CountDownLatch(1);
        mDomLoadedCallbackLatch = new CountDownLatch(1);

        mSetUpLatch.countDown();

        final String htmlEditor = Utils.getHtmlFromFile(activity, "android-editor.html");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView = new EditorWebView(mInstrumentation.getContext(), null);
                mWebView.addJavascriptInterface(new MockJsCallbackReceiver(), JS_CALLBACK_HANDLER);
                mWebView.loadDataWithBaseURL("file:///android_asset/", htmlEditor, "text/html", "utf-8", "");
                mSetUpLatch.countDown();
            }
        });
    }

    public void testInitialization() throws InterruptedException {
        // Wait for setUp() to finish initializing the WebView
        mSetUpLatch.await();

        // Identify this method to the MockJsCallbackReceiver
        mTestMethod = TestMethod.INIT;

        // Expecting three startup callbacks from the ZSS editor
        mCallbackLatch = new CountDownLatch(3);
        mCallbackSet = new HashSet<>();
        boolean callbacksReceived = mCallbackLatch.await(5, TimeUnit.SECONDS);
        assertTrue(callbacksReceived);

        Set<String> expectedSet = new HashSet<>();
        expectedSet.add("callback-new-field:id=zss_field_title");
        expectedSet.add("callback-new-field:id=zss_field_content");
        expectedSet.add("callback-dom-loaded:undefined");

        assertEquals(expectedSet, mCallbackSet);
    }

    private class MockJsCallbackReceiver {
        @JavascriptInterface
        public void executeCallback(String callbackId, String params) {
            if (callbackId.equals("callback-dom-loaded")) {
                // Notify test methods that the dom has loaded
                mDomLoadedCallbackLatch.countDown();
            }

            // Handle callbacks and count down latches according to the currently running test
            switch(mTestMethod) {
                case INIT:
                    if (callbackId.equals("callback-new-field") || callbackId.equals("callback-dom-loaded")) {
                        mCallbackSet.add(callbackId + ":" + params);
                        mCallbackLatch.countDown();
                    }
                    break;
                default:
                    throw(new RuntimeException("Unknown calling method"));
            }
        }
    }
}
