import android.test.InstrumentationTestCase;

import java.net.URI;
import java.net.URISyntaxException;

public class URITest extends InstrumentationTestCase {
    public void testGetHost1() {
        URI uri = null;
        try {
            uri = new URI("https://wordpress.com");
        } catch (URISyntaxException e) {}
        assertNotNull(uri);

        assertEquals("wordpress.com", uri.getHost());
    }

    public void testGetHost2() {
        URI uri = null;
        try {
            uri = new URI("http://a.com#.b.com/test");
        } catch (URISyntaxException e) {}
        assertNotNull(uri);

        assertEquals("a.com", uri.getHost());
    }

    public void testGetHost3() {
        URI uri = null;
        try {
            uri = new URI("https://a.com");
        } catch (URISyntaxException e) {}
        assertNotNull(uri);

        assertEquals("a.com", uri.getHost());
    }

    public void testGetHost4() {
        URI uri = null;
        try {
            uri = new URI("https://a.com/test#test");
        } catch (URISyntaxException e) {}
        assertNotNull(uri);

        assertEquals("a.com", uri.getHost());
    }

    public void testGetHost5() {
        URI uri = null;
        try {
            uri = new URI("a.com");
        } catch (URISyntaxException e) {}
        assertNotNull(uri);

        assertNull(uri.getHost());
    }
}
