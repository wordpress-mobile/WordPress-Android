package org.wordpress.android.editor;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.wordpress.android.util.AppLog;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.robolectric.shadows.ShadowLog.LogItem;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class JsCallbackReceiverTest {
    private final static String EDITOR_LOG_TAG = "WordPress-" + AppLog.T.EDITOR.toString();

    private JsCallbackReceiver mJsCallbackReceiver;

    @Before
    public void setUp() {
        EditorFragment editorFragment = mock(EditorFragment.class);
        mJsCallbackReceiver = new JsCallbackReceiver(editorFragment);
    }

    @Test
    public void testCallbacksRecognized() {
        mJsCallbackReceiver.executeCallback("callback-dom-loaded", "");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-new-field", "field-name");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-input", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-selection-changed", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-selection-style", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-focus-in", "");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-focus-out", "");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-image-replaced", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-image-tap", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-link-tap", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-log", "arguments");
        assertNotLogged("Unhandled callback");

        mJsCallbackReceiver.executeCallback("callback-response-string", "arguments");
        assertNotLogged("Unhandled callback");
    }

    @Test
    public void testUnknownCallbackShouldBeLogged() {
        mJsCallbackReceiver.executeCallback("callback-does-not-exist", "content");
        assertLogged(Log.DEBUG, EDITOR_LOG_TAG, "Unhandled callback: callback-does-not-exist:content", null);
    }

    @Test
    public void testCallbackLog() {
        mJsCallbackReceiver.executeCallback("callback-log", "msg=test-message");
        assertLogged(Log.DEBUG, EDITOR_LOG_TAG, "callback-log: test-message", null);
    }

    private void assertLogged(int type, String tag, String msg, Throwable throwable) {
        LogItem lastLog = ShadowLog.getLogs().get(0);
        assertEquals(type, lastLog.type);
        assertEquals(msg, lastLog.msg);
        assertEquals(tag, lastLog.tag);
        assertEquals(throwable, lastLog.throwable);
    }

    private void assertNotLogged(String msg) {
        List<LogItem> logList = ShadowLog.getLogs();
        if (!logList.isEmpty()) {
            assertFalse(logList.get(0).msg.contains(msg));
            ShadowLog.reset();
        }
    }
}
