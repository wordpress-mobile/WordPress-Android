package org.wordpress.android.util;

import android.test.InstrumentationTestCase;
import android.text.SpannableStringBuilder;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WPHtmlTest extends InstrumentationTestCase {
    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    // This test failed before #685 was fixed (throws a InvocationTargetException)
    public void testStartImg() throws NoSuchMethodException, IllegalAccessException {
        SpannableStringBuilder text = new SpannableStringBuilder();
        Attributes attributes = new AttributesImpl();

        HtmlToSpannedConverter converter = new HtmlToSpannedConverter(null, null, null, null, null, null, 0);

        // startImg is private, we use reflection to change accessibility and invoke it from here
        Method method = HtmlToSpannedConverter.class.getDeclaredMethod("startImg", SpannableStringBuilder.class,
                Attributes.class, WPHtml.ImageGetter.class);
        method.setAccessible(true);
        try {
            method.invoke(converter, text, attributes, null);
        } catch (InvocationTargetException e) {
            assertTrue("startImg failed see #685", false);
        }
    }
}