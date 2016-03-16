package org.wordpress.android.stores.network.xmlrpc;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
            Map<String, Object> map = (Map<String, Object>) XMLRPCSerializer.deserialize(pullParser);
            String faultString = (String) map.get(TAG_FAULT_STRING);
            int faultCode = (Integer) map.get(TAG_FAULT_CODE);
            throw new XMLRPCFault(faultString, faultCode);
        } else {
            throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
        }
    }
}
