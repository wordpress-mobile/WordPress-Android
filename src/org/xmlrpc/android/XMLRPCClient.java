package org.xmlrpc.android;

import java.io.File;
import java.io.FileWriter;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.wordpress.android.ConnectionClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class XMLRPCClient {
	private static final String TAG_METHOD_CALL = "methodCall";
	private static final String TAG_METHOD_NAME = "methodName";
	private static final String TAG_METHOD_RESPONSE = "methodResponse";
	private static final String TAG_PARAMS = "params";
	private static final String TAG_PARAM = "param";
	private static final String TAG_FAULT = "fault";
	private static final String TAG_FAULT_CODE = "faultCode";
	private static final String TAG_FAULT_STRING = "faultString";

	private ConnectionClient client;
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
		
		postMethod.addHeader("charset", "UTF-8");
		//UPDATE THE VERSION NUMBER BEFORE RELEASE! <3 Dan
		postMethod.addHeader("User-Agent", "wp-android/1.3");
		
		httpParams = postMethod.getParams();
		HttpProtocolParams.setUseExpectContinue(httpParams, false);
		
		//username & password not needed
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials("", "");
		
		//this gets connections working over https
		if (uri.getScheme() != null){
			if(uri.getScheme().equals("https")) { 
				if(uri.getPort() == -1)
					try {
						client = new ConnectionClient(creds, 443);
					} catch (KeyManagementException e) {
						client = new ConnectionClient(creds); 
					} catch (NoSuchAlgorithmException e) {
						client = new ConnectionClient(creds); 
					} catch (KeyStoreException e) {
						client = new ConnectionClient(creds); 
					} catch (UnrecoverableKeyException e) {
						client = new ConnectionClient(creds); 
					}
					else
						try {
							client = new ConnectionClient(creds, uri.getPort());
						} catch (KeyManagementException e) {
							client = new ConnectionClient(creds); 
						} catch (NoSuchAlgorithmException e) {
							client = new ConnectionClient(creds); 
						} catch (KeyStoreException e) {
							client = new ConnectionClient(creds); 
						} catch (UnrecoverableKeyException e) {
							client = new ConnectionClient(creds); 
						} 
			} 
			else {
				client = new ConnectionClient(creds); 
			}
		}
		else{
			client = new ConnectionClient(creds);
		}
		
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
			File tempFile = null;
			if (method.equals("wp.uploadFile")){
				String tempFilePath = Environment.getExternalStorageDirectory() + File.separator + "wordpress" + File.separator + "wp-" + System.currentTimeMillis() + ".xml";
				
				File directory = new File(tempFilePath).getParentFile();
	            if (!directory.exists() && !directory.mkdirs()) {
	            	throw new XMLRPCException("Path to file could not be created.");
	            }

				tempFile = new File(tempFilePath);
				FileWriter fileWriter = new FileWriter(tempFile);
				serializer.setOutput(fileWriter);
				
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
				
				fileWriter.flush();
				fileWriter.close();
				FileEntity fEntity = new FileEntity(tempFile,"text/xml; charset=\"UTF-8\""); 
	            fEntity.setContentType("text/xml");
	            //fEntity.setChunked(true);
				long fileSize = tempFile.length();
	            postMethod.setEntity(fEntity);
	            
	            
			}
			else{
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
				
				HttpEntity entity = new StringEntity(bodyWriter.toString());
				//Log.i("WordPress", bodyWriter.toString());
				postMethod.setEntity(entity);
			}

			//set timeout to 40 seconds, does it need to be set for both client and method?
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 40000);
	        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 40000);
			postMethod.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 40000);
			postMethod.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 40000);
			
			// execute HTTP POST request
			HttpResponse response = client.execute(postMethod);
			
			Log.i("WordPress", "response = " + response.getStatusLine());
			// check status code
			int statusCode = response.getStatusLine().getStatusCode();
			
			if ((method.equals("wp.uploadFile"))){ //get rid of the temp file
				tempFile.delete();
			}
			
			if (statusCode != HttpStatus.SC_OK) {
				throw new XMLRPCException("HTTP status code: " + statusCode + " was returned. " + response.getStatusLine().getReasonPhrase());
			}

			// setup pull parser
			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			HttpEntity entity = response.getEntity();
			//change to pushbackinput stream 1/18/2010 to handle self installed wp sites that insert the BOM
			PushbackInputStream is = new PushbackInputStream(entity.getContent());
			
			//get rid of junk characters before xml response.  60 = '<'.  Added stopper to prevent infinite loop
			int bomCheck = is.read();
			int stopper = 0;
			while (bomCheck != 60 && stopper < 20){
				bomCheck = is.read();
				stopper++;
			}
			is.unread(bomCheck);
			
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
