package org.xmlrpc.android;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.wordpress.android.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Xml;
import android.widget.TextView;


/**
 * XMLRPCClient allows to call remote XMLRPC method.
 * 
 * <p>
 * The following table shows how XML-RPC types are mapped to java call parameters/response values.
 * </p>
 * 
 * <p>
 * <table border="2" align="center" cellpadding="5">
 * <thead><tr><th>XML-RPC Type</th><th>Call Parameters</th><th>Call Response</th></tr></thead>
 * 
 * <tbody>
 * <td>int, i4</td><td>byte<br />Byte<br />short<br />Short<br />int<br />Integer</td><td>int<br />Integer</td>
 * </tr>
 * <tr>
 * <td>i8</td><td>long<br />Long</td><td>long<br />Long</td>
 * </tr>
 * <tr>
 * <td>double</td><td>float<br />Float<br />double<br />Double</td><td>double<br />Double</td>
 * </tr>
 * <tr>
 * <td>string</td><td>String</td><td>String</td>
 * </tr>
 * <tr>
 * <td>boolean</td><td>boolean<br />Boolean</td><td>boolean<br />Boolean</td>
 * </tr>
 * <tr>
 * <td>dateTime.iso8601</td><td>java.util.Date<br />java.util.Calendar</td><td>java.util.Date</td>
 * </tr>
 * <tr>
 * <td>base64</td><td>byte[]</td><td>byte[]</td>
 * </tr>
 * <tr>
 * <td>array</td><td>java.util.List&lt;Object&gt;<br />Object[]</td><td>Object[]</td>
 * </tr>
 * <tr>
 * <td>struct</td><td>java.util.Map&lt;String, Object&gt;</td><td>java.util.Map&lt;String, Object&gt;</td>
 * </tr>
 * </tbody>
 * </table>
 * </p>
 */

public class XMLRPCClient {
	private static final String TAG_METHOD_CALL = "methodCall";
	private static final String TAG_METHOD_NAME = "methodName";
	private static final String TAG_METHOD_RESPONSE = "methodResponse";
	private static final String TAG_PARAMS = "params";
	private static final String TAG_PARAM = "param";
	private static final String TAG_FAULT = "fault";
	private static final String TAG_FAULT_CODE = "faultCode";
	private static final String TAG_FAULT_STRING = "faultString";

	private HttpClient client;
	private HttpPost postMethod;
	private XmlSerializer serializer;
	private HttpParams httpParams;

	/**
	 * XMLRPCClient constructor. Creates new instance based on server URI
	 * @param XMLRPC server URI
	 */
	public XMLRPCClient(URI uri) {
		postMethod = new HttpPost(uri);
		postMethod.addHeader("Content-Type", "text/xml");		
		//UPDATE THE VERSION NUMBER BEFORE RELEASE!
		postMethod.addHeader("User-Agent", "wp-android/0.9.1");
		
		// WARNING
		// I had to disable "Expect: 100-Continue" header since I had 
		// two second delay between sending http POST request and POST body 
		httpParams = postMethod.getParams();
		HttpProtocolParams.setUseExpectContinue(httpParams, false);
		
		client = new DefaultHttpClient();
		serializer = Xml.newSerializer();
	}
	
	/**
	 * Convenience constructor. Creates new instance based on server String address
	 * @param XMLRPC server address
	 */
	public XMLRPCClient(String url) {
		this(URI.create(url));
	}
	
	/**
	 * Convenience XMLRPCClient constructor. Creates new instance based on server URL
	 * @param XMLRPC server URL
	 */
	public XMLRPCClient(URL url) {
		this(URI.create(url.toExternalForm()));
	}

	/**
	 * Call method with optional parameters. This is general method.
	 * If you want to call your method with 0-8 parameters, you can use more
	 * convenience call methods
	 * 
	 * @param method name of method to call
	 * @param params parameters to pass to method (may be null if method has no parameters)
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object[] params) throws XMLRPCException {
		return callXMLRPC(method, params);
	}
	
	/**
	 * Convenience method call with no parameters
	 * 
	 * @param method name of method to call
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method) throws XMLRPCException {
		return callXMLRPC(method, null);
	}
	
	/**
	 * Convenience method call with one parameter
	 * 
	 * @param method name of method to call
	 * @param p0 method's parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0) throws XMLRPCException {
		Object[] params = {
			p0,
		};
		return callXMLRPC(method, params);
	}
	
	/**
	 * Convenience method call with two parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1) throws XMLRPCException {
		Object[] params = {
			p0, p1,
		};
		return callXMLRPC(method, params);
	}
	
	/**
	 * Convenience method call with three parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Convenience method call with four parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @param p3 method's 4th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2, Object p3) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2, p3,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Convenience method call with five parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @param p3 method's 4th parameter
	 * @param p4 method's 5th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2, Object p3, Object p4) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2, p3, p4,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Convenience method call with six parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @param p3 method's 4th parameter
	 * @param p4 method's 5th parameter
	 * @param p5 method's 6th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2, p3, p4, p5,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Convenience method call with seven parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @param p3 method's 4th parameter
	 * @param p4 method's 5th parameter
	 * @param p5 method's 6th parameter
	 * @param p6 method's 7th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2, p3, p4, p5, p6,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Convenience method call with eight parameters
	 * 
	 * @param method name of method to call
	 * @param p0 method's 1st parameter
	 * @param p1 method's 2nd parameter
	 * @param p2 method's 3rd parameter
	 * @param p3 method's 4th parameter
	 * @param p4 method's 5th parameter
	 * @param p5 method's 6th parameter
	 * @param p6 method's 7th parameter
	 * @param p7 method's 8th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(String method, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) throws XMLRPCException {
		Object[] params = {
			p0, p1, p2, p3, p4, p5, p6, p7,
		};
		return callXMLRPC(method, params);
	}

	/**
	 * Call method with optional parameters
	 * 
	 * @param method name of method to call
	 * @param params parameters to pass to method (may be null if method has no parameters)
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	private Object callXMLRPC(String method, Object[] params) throws XMLRPCException {
		try {
			// prepare POST body
			StringWriter bodyWriter = new StringWriter();
			serializer.setOutput(bodyWriter);
			serializer.startDocument(null, null);
			serializer.startTag(null, TAG_METHOD_CALL);
			// set method name
			serializer.startTag(null, TAG_METHOD_NAME).text(method).endTag(null, TAG_METHOD_NAME);
			if (params != null && params.length != 0) {
				// set method params
				serializer.startTag(null, TAG_PARAMS);
				for (int i=0; i<params.length; i++) {
					serializer.startTag(null, TAG_PARAM).startTag(null, XMLRPCSerializer.TAG_VALUE);
					XMLRPCSerializer.serialize(serializer, params[i]);
					serializer.endTag(null, XMLRPCSerializer.TAG_VALUE).endTag(null, TAG_PARAM);
				}
				serializer.endTag(null, TAG_PARAMS);
			}
			serializer.endTag(null, TAG_METHOD_CALL);
			serializer.endDocument();

			// set POST body
			HttpEntity entity = new StringEntity(bodyWriter.toString());
			postMethod.setEntity(entity);
			
			// execute HTTP POST request
			HttpResponse response = client.execute(postMethod);

			// check status code
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new XMLRPCException("HTTP status code: " + statusCode + " != " + HttpStatus.SC_OK);
			}

			// parse response stuff
			//
			// setup pull parser
			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			entity = response.getEntity();
			//change to pushbackinput stream 1/18/2010 to handle self installed wp sites that insert the BOM
			PushbackInputStream is = new PushbackInputStream(entity.getContent());
			int bomCheck = is.read();			
			if (bomCheck != 239){
				is.unread(bomCheck);
			}
			else{
				is.skip(2);
			}
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
				Object obj = XMLRPCSerializer.deserialize(pullParser);
				entity.consumeContent();
				return obj;
			} else
			if (tag.equals(TAG_FAULT)) {
				// fault response
				pullParser.nextTag(); // TAG_VALUE (<value>)
				// no parser.require() here since its called in XMLRPCSerializer.deserialize() below

				// deserialize fault result
				Map<String, Object> map = (Map<String, Object>) XMLRPCSerializer.deserialize(pullParser);
				String faultString = (String) map.get(TAG_FAULT_STRING);
				int faultCode = (Integer) map.get(TAG_FAULT_CODE);
				entity.consumeContent();
				throw new XMLRPCFault(faultString, faultCode);
			} else {
				entity.consumeContent();
				throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
			}
		} catch (XMLRPCException e) {
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} catch (Exception e) {
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}
	}
}
