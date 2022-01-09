package org.wordpress.android.util;

import android.content.Context;
import android.text.SpannableStringBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;

import static junit.framework.TestCase.assertTrue;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class WPHtmlTest {
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject Context mContext;

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    // This test failed before #685 was fixed (throws a InvocationTargetException)
    @Test
    public void testStartImg() throws NoSuchMethodException, IllegalAccessException {
        SpannableStringBuilder text = new SpannableStringBuilder();
        Attributes attributes = new AttributesImpl();

        HtmlToSpannedConverter converter = new HtmlToSpannedConverter(
                null,
                null,
                null,
                null,
                mContext,
                null,
                0);

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
