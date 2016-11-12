import android.test.InstrumentationTestCase;

import java.net.MalformedURLException;
import java.net.URL;

public class URLTest extends InstrumentationTestCase {
    public void testGetHost1() {
        URL url = null;
        try {
            url = new URL("https://wordpress.com");
        } catch (MalformedURLException e) {}
        assertNotNull(url);

        assertEquals("wordpress.com", url.getHost());
    }

    public void testGetHost2() {
        URL url = null;
        try {
            url = new URL("http://a.com#.b.com/test");
        } catch (MalformedURLException e) {}
        assertNotNull(url);

        assertEquals("a.com", url.getHost());
    }

    public void testGetHost3() {
        URL url = null;
        try {
            url = new URL("https://a.com");
        } catch (MalformedURLException e) {}
        assertNotNull(url);

        assertEquals("a.com", url.getHost());
    }

    public void testGetHost4() {
        URL url = null;
        try {
            url = new URL("https://a.com/test#test");
        } catch (MalformedURLException e) {}
        assertNotNull(url);

        assertEquals("a.com", url.getHost());
    }

    public void testGetHost5() {
        URL url = null;
        try {
            url = new URL("a.com");
        } catch (MalformedURLException e) {}

        assertNull(url);
    }
}
