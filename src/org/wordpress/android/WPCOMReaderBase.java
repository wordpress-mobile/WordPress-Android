package org.wordpress.android;



import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Context;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public abstract class WPCOMReaderBase extends Activity {
	
	protected final String interfaceNameForJS = "Android";
	
	protected String httpuser = "";
	protected String httppassword = "";
	
	protected void setDefaultWebViewSettings(WebView wv) {
		WebSettings webSettings = wv.getSettings();
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setUserAgentString("wp-android");
		webSettings.setSavePassword(false);
	}
	
	protected String getAuthorizeHybridURL(String URL) {
		
		if( ! isValidHybridURL(URL) ) return URL;
		
		if( URL.contains("?") )
			return URL + "&wpcom-hybrid-auth-token=" + URLEncoder.encode( this.getHybridAuthToken() );
		else 
			return URL + "?wpcom-hybrid-auth-token=" + URLEncoder.encode( this.getHybridAuthToken() );
	}
	
	protected boolean isValidHybridURL(String URL) {
		return URL.contains(Constants.authorizedHybridHost);
	}

	protected String getHybridAuthToken() {
		// gather all of the device info
		String uuid = WordPress.wpDB.getUUID(this);
		if (uuid == "") {
			uuid = UUID.randomUUID().toString();
			WordPress.wpDB.updateUUID(uuid);
		}
		return uuid;
	}
	
	protected class WordPressWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// setProgressBarIndeterminateVisibility(false);
			view.clearCache(true);
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			handler.proceed();
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			handler.proceed(httpuser, httppassword);
		}
	}
	
	
	protected class JavaScriptInterface {
	    Context mContext;

	    /** Instantiate the interface and set the context */
	    JavaScriptInterface(Context c) {
	        mContext = c;
	    }

	    public void callNative(String payload) {
	    	Log.v("callNative",payload );
	    	String jsonString = payload.substring(0, payload.indexOf("&wpcom-hybrid-auth-token="));
	    	String wpcom_hybrid_auth_token= payload.substring(payload.indexOf("&wpcom-hybrid-auth-token="), payload.length() );
	    	wpcom_hybrid_auth_token = wpcom_hybrid_auth_token.replace("&wpcom-hybrid-auth-token=", "");
	    	
	    	WPCOMReaderBase parentClass = WPCOMReaderBase.this;
	    	
	    	if ( ! wpcom_hybrid_auth_token.equals( parentClass.getHybridAuthToken() ) ) {
	    		//Token miss-match
	    		Log.e (  "Remote native call failed", "Token missmatch" );
	    		return;
	    	}
	    	
	    	//Call the right methods by using reflection. Oh Java, we love you.
	    	try {
	    		JSONArray methodsToCall = (JSONArray) new JSONTokener(jsonString).nextValue();
	    		//a single call from the JS code can contain the invocation of more than one native method
	    		for (int i = 0; i < methodsToCall.length(); i++) { 
	    			
	    			JSONObject currentMethodToCall = methodsToCall.getJSONObject(i);
	    			String methodName = currentMethodToCall.getString("method");
	    			methodName = methodName+"FromJS"; //Append a prefix so we are sure what that method is for in the java code.
	    			JSONArray args = currentMethodToCall.getJSONArray("args");
	    			
	    			Object[] formalParameters = new Object[args.length()];   //declares the parameters to be passed to the method
	    			Class[] formalParametersType = new Class[args.length()]; //declares the parameters type the method takes
	    			
	    			for (int j = 0; j < args.length(); j++) {
	    				formalParameters[j] = args.getString(j); //We know that for now only String parameters are allowed
	    				formalParametersType[j] = String.class;
	    			}

	    			//call the java method by using reflection
	    			java.lang.reflect.Method method;
	    			try {
	    				method = parentClass.getClass().getMethod( methodName, formalParametersType );
	    				method.invoke( parentClass, formalParameters );
	    			} catch (SecurityException e) {
	    				Log.e(parentClass.getClass().getName(), "Exception getting method name", e); 
	    			} catch (NoSuchMethodException e) {
	    				//This could happen when the JS code try to call a method not defined in the class. For ex when it is calling a method for the iOS app.
	    				Log.w(parentClass.getClass().getName(), "Method not found in the class: " + methodName );
	    				//Log.w(parentClass.getClass().getName(), "Method not found in the class", e);
	    			}
	    			catch (IllegalArgumentException e) {
	    				Log.e(parentClass.getClass().getName(), "Method called with invalid arguments", e);
	    			} catch (IllegalAccessException e) {
	    				Log.e(parentClass.getClass().getName(), "Method is not accessible", e);
	    			} catch (InvocationTargetException e) {
	    				Log.e(parentClass.getClass().getName(), "Object can't call this method", e);
	    			}
	    		}
			} catch (JSONException e) {
				Log.e (  "Calling native call from JS failed." , Log.getStackTraceString( e ) );
			}
	    }
	}
	
}
