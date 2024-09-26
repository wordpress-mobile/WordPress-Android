package org.wordpress.android.fluxc.network.xmlrpc;

import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class XMLSerializerUtils {
    private static final String TAG_METHOD_CALL = "methodCall";
    private static final String TAG_METHOD_NAME = "methodName";
    private static final String TAG_METHOD_RESPONSE = "methodResponse";
    private static final String TAG_PARAMS = "params";
    private static final String TAG_PARAM = "param";
    private static final String TAG_FAULT = "fault";
    private static final String TAG_FAULT_CODE = "faultCode";
    private static final String TAG_FAULT_STRING = "faultString";

    private static final int MAX_SCRUB_CHARACTERS = 5000;

    public static StringWriter serialize(XmlSerializer serializer, XMLRPC method, Object[] params)
            throws IOException {
        StringWriter bodyWriter = new StringWriter();
        serializer.setOutput(bodyWriter);

        serializer.startDocument(null, null);
        serializer.startTag(null, TAG_METHOD_CALL);
        // set method name
        serializer.startTag(null, TAG_METHOD_NAME).text(method.toString()).endTag(null, TAG_METHOD_NAME);
        if (params != null && params.length != 0) {
            // set method params
            serializer.startTag(null, TAG_PARAMS);
            for (int i = 0; i < params.length; i++) {
                serializer.startTag(null, TAG_PARAM).startTag(null, XMLRPCSerializer.TAG_VALUE);
                XMLRPCSerializer.serialize(serializer, params[i]);
                serializer.endTag(null, XMLRPCSerializer.TAG_VALUE).endTag(null, TAG_PARAM);
            }
            serializer.endTag(null, TAG_PARAMS);
        }
        serializer.endTag(null, TAG_METHOD_CALL);
        serializer.endDocument();

        return bodyWriter;
    }

    public static Object deserialize(InputStream is)
            throws IOException, XmlPullParserException, XMLRPCException {
        // setup pull parser
        XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
        pullParser.setInput(is, "UTF-8");

        // lets start pulling...
        pullParser.nextTag();
        pullParser.require(XmlPullParser.START_TAG, null, TAG_METHOD_RESPONSE);

        pullParser.nextTag(); // either TAG_PARAMS (<params>) or TAG_FAULT (<fault>)
        String tag = pullParser.getName();
        if (tag.equals(TAG_PARAMS)) {
            // normal response
            pullParser.nextTag(); // TAG_PARAM (<param>)
            pullParser.require(XmlPullParser.START_TAG, null, TAG_PARAM);
            pullParser.nextTag(); // TAG_VALUE (<value>)
            // no parser.require() here since its called in XMLRPCSerializer.deserialize() below
            // deserialize result
            return XMLRPCSerializer.deserialize(pullParser);
        } else if (tag.equals(TAG_FAULT)) {
            // fault response
            pullParser.nextTag(); // TAG_VALUE (<value>)
            // no parser.require() here since its called in XMLRPCSerializer.deserialize() below
            // deserialize fault result
            Map<?, ?> map = (Map<?, ?>) XMLRPCSerializer.deserialize(pullParser);
            String faultString = XMLRPCUtils.safeGetMapValue(map, TAG_FAULT_STRING, "");
            int faultCode = XMLRPCUtils.safeGetMapValue(map, TAG_FAULT_CODE, 0);
            throw new XMLRPCFault(faultString, faultCode);
        } else {
            throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
        }
    }

    public static InputStream scrubXmlResponse(InputStream is) throws IOException {
        // Many WordPress configs can output junk before the xml response (php warnings for example), this cleans it.
        int bomCheck = -1;
        int stopper = 0;
        while ((bomCheck = is.read()) != -1 && stopper <= MAX_SCRUB_CHARACTERS) {
            stopper++;
            String snippet = "";
            // 60 == '<' character
            if (bomCheck == 60) {
                for (int i = 0; i < 4; i++) {
                    byte[] chunk = new byte[1];
                    int numRead = is.read(chunk);
                    if (numRead > 0) {
                        snippet += new String(chunk, "UTF-8");
                    }
                }
                if (snippet.equals("?xml")) {
                    // it's all good, add xml tag back and start parsing
                    String start = "<" + snippet;
                    List<InputStream> streams = Arrays.asList(new ByteArrayInputStream(start.getBytes()), is);
                    is = new SequenceInputStream(Collections.enumeration(streams));
                    break;
                } else {
                    // keep searching...
                    List<InputStream> streams = Arrays.asList(new ByteArrayInputStream(snippet.getBytes()), is);
                    is = new SequenceInputStream(Collections.enumeration(streams));
                }
            }
        }

        return is;
    }
}
