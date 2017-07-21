package org.wordpress.android.fluxc.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCSerializer;
import org.wordpress.android.fluxc.network.xmlrpc.XMLSerializerUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
public class XMLSerializerUtilsTest {
    @Test
    public void testXmlRpcResponseScrubWithJunk() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nothing></nothing>";
        final String junk = "this is junk text 12345,./;'pp<<><><;;";
        final String result = scrub(junk + xml, xml.length());
        Assert.assertEquals(xml, result);
    }

    @Test
    public void testXmlRpcResponseScrubWithPhpWarning() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nothing></nothing>";
        final String junk1 = "Warning: virtual() [function.virtual2]: Unable to include '/cgi-bin/script/l' - request"
                             + " execution failed in /home/mysite/public_html/index.php on line 2\n";
        final String junk2 = "Warning: virtual() [function.virtual2]: Unable to include '/cgi-bin/script/l' - request"
                             + " execution failed in /home/mysite/public_html/index.php on line 3\n";
        final String result = scrub(junk1 + junk2 + xml, xml.length());
        Assert.assertEquals(xml, result);
    }

    @Test
    public void testXmlRpcResponseScrubWithoutJunk() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nothing></nothing>";
        final String result = scrub(xml, xml.length());
        Assert.assertEquals(xml, result);
    }

    private String scrub(String input, int xmlLength) {
        try {
            final InputStream is = new ByteArrayInputStream(input.getBytes("UTF-8"));
            final InputStream resultStream = XMLSerializerUtils.scrubXmlResponse(is);
            byte[] bb = new byte[xmlLength];
            int val;
            for (int i = 0; i < bb.length && ((val = resultStream.read()) != -1); ++i) {
                bb[i] = (byte) val;
            }

            is.close();
            resultStream.close();

            return new String(bb, "UTF-8");
        } catch (IOException e) {
        }
        return null;
    }

    @Test
    public void testXMLRPCSerializer_makeValidInputString_emoji() throws IOException {
        // Not a XML 1.0 valid character
        String inputString = "\uD83D";
        String serializeThis = XMLRPCSerializer.makeValidInputString(inputString);
        // If the input wasn't modified, it will fail during the XMLRPC serialization step
        Assert.assertNotEquals(inputString, serializeThis);
    }
}
