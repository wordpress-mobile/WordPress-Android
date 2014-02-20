package org.xmlrpc.android;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SimpleTimeZone;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import org.wordpress.android.models.MediaFile;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

class XMLRPCSerializer {
    static final String TAG_NAME = "name";
    static final String TAG_MEMBER = "member";
    static final String TAG_VALUE = "value";
    static final String TAG_DATA = "data";

    static final String TYPE_INT = "int";
    static final String TYPE_I4 = "i4";
    static final String TYPE_I8 = "i8";
    static final String TYPE_DOUBLE = "double";
    static final String TYPE_BOOLEAN = "boolean";
    static final String TYPE_STRING = "string";
    static final String TYPE_DATE_TIME_ISO8601 = "dateTime.iso8601";
    static final String TYPE_BASE64 = "base64";
    static final String TYPE_ARRAY = "array";
    static final String TYPE_STRUCT = "struct";

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    static Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
    
    private static final XmlSerializer serializeTester;

    static {
        serializeTester = Xml.newSerializer();
        try {
            serializeTester.setOutput(new NullOutputStream(), "UTF-8");
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.EDITOR, "IllegalArgumentException setting test serializer output stream", e );
        } catch (IllegalStateException e) {
            AppLog.e(AppLog.T.EDITOR, "IllegalStateException setting test serializer output stream", e );
        } catch (IOException e) {
            AppLog.e(AppLog.T.EDITOR, "IOException setting test serializer output stream", e );
        }
    }

    @SuppressWarnings("unchecked")
    static void serialize(XmlSerializer serializer, Object object) throws IOException {
        // check for scalar types:
        if (object instanceof Integer || object instanceof Short || object instanceof Byte) {
            serializer.startTag(null, TYPE_I4).text(object.toString()).endTag(null, TYPE_I4);
        } else
        if (object instanceof Long) {
            serializer.startTag(null, TYPE_I8).text(object.toString()).endTag(null, TYPE_I8);
        } else
        if (object instanceof Double || object instanceof Float) {
            serializer.startTag(null, TYPE_DOUBLE).text(object.toString()).endTag(null, TYPE_DOUBLE);
        } else
        if (object instanceof Boolean) {
            Boolean bool = (Boolean) object;
            String boolStr = bool.booleanValue() ? "1" : "0";
            serializer.startTag(null, TYPE_BOOLEAN).text(boolStr).endTag(null, TYPE_BOOLEAN);
        } else
        if (object instanceof String) {
            serializer.startTag(null, TYPE_STRING).text(makeValidInputString((String) object)).endTag(null, TYPE_STRING);
        } else
        if (object instanceof Date || object instanceof Calendar) {
            Date date = (Date) object;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
            dateFormat.setCalendar(cal);
            String sDate = dateFormat.format(date);
            serializer.startTag(null, TYPE_DATE_TIME_ISO8601).text(sDate).endTag(null, TYPE_DATE_TIME_ISO8601);
        } else
        if (object instanceof byte[] ){
            String value;
            try {
                value = Base64.encodeToString((byte[])object, Base64.DEFAULT);
                serializer.startTag(null, TYPE_BASE64).text(value).endTag(null, TYPE_BASE64);
            } catch (OutOfMemoryError e) {
                throw new IOException("Out of memory");
            }
        }
        else if( object instanceof MediaFile ) {
            //convert media file binary to base64
            serializer.startTag( null, "base64" );
            MediaFile videoFile = (MediaFile) object;
            InputStream inStream = new DataInputStream(new FileInputStream(videoFile.getFilePath()));
            byte[] buffer = new byte[3600];//you must use a 24bit multiple
            int length = -1;
            String chunk = null;
            while ((length = inStream.read(buffer)) > 0) {
                chunk = Base64.encodeToString(buffer, 0, length, Base64.DEFAULT);
                serializer.text(chunk);
            }
            inStream.close();
            serializer.endTag(null, "base64");
        }else
        if (object instanceof List<?>) {
            serializer.startTag(null, TYPE_ARRAY).startTag(null, TAG_DATA);
            List<Object> list = (List<Object>) object;
            Iterator<Object> iter = list.iterator();
            while (iter.hasNext()) {
                Object o = iter.next();
                serializer.startTag(null, TAG_VALUE);
                serialize(serializer, o);
                serializer.endTag(null, TAG_VALUE);
            }
            serializer.endTag(null, TAG_DATA).endTag(null, TYPE_ARRAY);
        } else
        if (object instanceof Object[]) {
            serializer.startTag(null, TYPE_ARRAY).startTag(null, TAG_DATA);
            Object[] objects = (Object[]) object;
            for (int i=0; i<objects.length; i++) {
                Object o = objects[i];
                serializer.startTag(null, TAG_VALUE);
                serialize(serializer, o);
                serializer.endTag(null, TAG_VALUE);
            }
            serializer.endTag(null, TAG_DATA).endTag(null, TYPE_ARRAY);
        } else
        if (object instanceof Map) {
            serializer.startTag(null, TYPE_STRUCT);
            Map<String, Object> map = (Map<String, Object>) object;
            Iterator<Entry<String, Object>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, Object> entry = iter.next();
                String key = entry.getKey();
                Object value = entry.getValue();

                serializer.startTag(null, TAG_MEMBER);
                serializer.startTag(null, TAG_NAME).text(key).endTag(null, TAG_NAME);
                serializer.startTag(null, TAG_VALUE);
                serialize(serializer, value);
                serializer.endTag(null, TAG_VALUE);
                serializer.endTag(null, TAG_MEMBER);
            }
            serializer.endTag(null, TYPE_STRUCT);
        } else {
            throw new IOException("Cannot serialize " + object);
        }
    }

    private static final String makeValidInputString(final String input) throws IOException {
        if (TextUtils.isEmpty(input))
            return "";

        if (serializeTester == null)
            return input;

        try {
            // try to encode the string as-is, 99.9% of the time it's OK
            serializeTester.text(input);
            return input;
        } catch (IllegalArgumentException e) {
            // There are characters outside the XML unicode charset as specified by the XML 1.0 standard
            // See http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char
            AppLog.e(AppLog.T.EDITOR, "There are characters outside the XML unicode charset as specified by the XML 1.0 standard", e );
        }

        // We need to do the following things:
        // 1. Replace surrogates with HTML Entity.
        // 2. Replace emoji with their textual versions (if available on WP)
        // 3. Try to serialize the resulting string.
        // 4. If it fails again, strip characters that are not allowed in XML 1.0
        final String noEmojiString = StringUtils.replaceUnicodeSurrogateBlocksWithHTMLEntities(input);
        try {
            serializeTester.text(noEmojiString);
            return noEmojiString;
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.EDITOR, "noEmojiString still contains characters outside the XML unicode charset as specified by the XML 1.0 standard", e );
            return StringUtils.stripNonValidXMLCharacters(noEmojiString);
        }
    }
    
    static Object deserialize(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, TAG_VALUE);

        parser.nextTag();
        String typeNodeName = parser.getName();

        Object obj;
        if (typeNodeName.equals(TYPE_INT) || typeNodeName.equals(TYPE_I4)) {
            String value = parser.nextText();
            obj = Integer.parseInt(value);
        } else
        if (typeNodeName.equals(TYPE_I8)) {
            String value = parser.nextText();
            obj = Long.parseLong(value);
        } else
        if (typeNodeName.equals(TYPE_DOUBLE)) {
            String value = parser.nextText();
            obj = Double.parseDouble(value);
        } else
        if (typeNodeName.equals(TYPE_BOOLEAN)) {
            String value = parser.nextText();
            obj = value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
        } else
        if (typeNodeName.equals(TYPE_STRING)) {
            obj = parser.nextText();
        } else
        if (typeNodeName.equals(TYPE_DATE_TIME_ISO8601)) {
            dateFormat.setCalendar(cal);
            String value = parser.nextText();
            try {
                obj = dateFormat.parseObject(value);
            } catch (ParseException e) {
                e.printStackTrace();
                obj = value;
                //throw new IOException("Cannot deserialize dateTime " + value);
            }
        } else
        if (typeNodeName.equals(TYPE_BASE64)) {
            String value = parser.nextText();
            BufferedReader reader = new BufferedReader(new StringReader(value));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            obj = Base64.decode(sb.toString(), Base64.DEFAULT);
        } else
        if (typeNodeName.equals(TYPE_ARRAY)) {
            parser.nextTag(); // TAG_DATA (<data>)
            parser.require(XmlPullParser.START_TAG, null, TAG_DATA);

            parser.nextTag();
            List<Object> list = new ArrayList<Object>();
            while (parser.getName().equals(TAG_VALUE)) {
                list.add(deserialize(parser));
                parser.nextTag();
            }
            parser.require(XmlPullParser.END_TAG, null, TAG_DATA);
            parser.nextTag(); // TAG_ARRAY (</array>)
            parser.require(XmlPullParser.END_TAG, null, TYPE_ARRAY);
            obj = list.toArray();
        } else
        if (typeNodeName.equals(TYPE_STRUCT)) {
            parser.nextTag();
            Map<String, Object> map = new HashMap<String, Object>();
            while (parser.getName().equals(TAG_MEMBER)) {
                String memberName = null;
                Object memberValue = null;
                while (true) {
                    parser.nextTag();
                    String name = parser.getName();
                    if (name.equals(TAG_NAME)) {
                        memberName = parser.nextText();
                    } else
                    if (name.equals(TAG_VALUE)) {
                        memberValue = deserialize(parser);
                    } else {
                        break;
                    }
                }
                if (memberName != null && memberValue != null) {
                    map.put(memberName, memberValue);
                }
                parser.require(XmlPullParser.END_TAG, null, TAG_MEMBER);
                parser.nextTag();
            }
            parser.require(XmlPullParser.END_TAG, null, TYPE_STRUCT);
            obj = map;
        } else {
            throw new IOException("Cannot deserialize " + parser.getName());
        }
        parser.nextTag(); // TAG_VALUE (</value>)
        parser.require(XmlPullParser.END_TAG, null, TAG_VALUE);
        return obj;
    }
}
