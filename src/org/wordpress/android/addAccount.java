package org.wordpress.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.conn.HttpHostConnectException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;



public class addAccount extends Activity {
	private XMLRPCClient client;
	public boolean success = false;
	public String blogURL, xmlrpcURL;
	public ProgressDialog pd;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_account);	
		
		this.setTitle("WordPress - " + getResources().getText(R.string.new_account));
        
        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button saveButton = (Button) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	

				pd = ProgressDialog.show(addAccount.this, getResources().getText(R.string.account_setup), getResources().getText(R.string.attempting_configure), true, false);

				
				Thread action = new Thread() 
				{ 
				  public void run() 
				  {
					  Looper.prepare();
					  configureAccount();
					  Looper.loop();
				  } 
				}; 
				action.start();
 
            }
        });   
        
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("returnStatus", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
        
        Button signUp = (Button) findViewById(R.id.wordpressdotcom);
        
        signUp.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	//Intent signupIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://wordpress.com/signup/?ref=wp-android")); 
            	Intent signupIntent = new Intent(addAccount.this, signup.class); 
            	startActivity(signupIntent);
            }
        }); 
	}
	
	protected void configureAccount() {
		
		//capture the entered fields *needs validation*
        EditText urlET = (EditText)findViewById(R.id.url);
        blogURL = urlET.getText().toString();
        EditText usernameET = (EditText)findViewById(R.id.username);
        final String username = usernameET.getText().toString();
        EditText passwordET = (EditText)findViewById(R.id.password);
        final String password = passwordET.getText().toString();
        
        if (blogURL.equals("") || username.equals("") || password.equals("")){
        	pd.dismiss();
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(addAccount.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.required_fields));
            dialogBuilder.setMessage(getResources().getText(R.string.url_username_password_required));
            dialogBuilder.setPositiveButton("OK",  new
          		  DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	
                }
            });
            dialogBuilder.setCancelable(true);
           dialogBuilder.create().show();
        }
        else{
        
        //add http  or http to the beginning of the URL if needed
        if (!(blogURL.toLowerCase().contains("http://")) && !(blogURL.toLowerCase().contains("https://"))){
        	blogURL = "http://" + blogURL;  //default to http
        }
        
        String fBlogURL = "";
        //attempt to get the XMLRPC URL via RSD

        String rsdUrl = getRSDMetaTagHref(blogURL);
		  
   
		if (rsdUrl != null){
        	xmlrpcURL = getXMLRPCUrl(rsdUrl);
        }

		
        if (xmlrpcURL != null){
        	//got the xmlrpc path ok!
        	fBlogURL = xmlrpcURL;
        }
        else{
        	//let's try to guess, and if it doesn't work prompt the user to type in the path manually after the call.
        	String lastChar = blogURL.substring(blogURL.length() - 1, blogURL.length());

        	if (lastChar.equals("/")){
        		blogURL = blogURL.substring(0, blogURL.length() - 1);
        	}
        
        	fBlogURL = blogURL + "/xmlrpc.php";
        }
        
        //verify settings
        client = new XMLRPCClient(fBlogURL);
    	
    	XMLRPCMethod method = new XMLRPCMethod("wp.getUsersBlogs", new XMLRPCMethodCallback() {
			public void callFinished(Object[] result) {
				
				
				String[] blogNames = new String[result.length];
				String[] urls = new String[result.length];
				int[]blogIds = new int[result.length];
				
				HashMap contentHash = new HashMap();
				    
				int ctr = 0;
				
				//loop this!
				    for (Object item : result){
				        contentHash = (HashMap) result[ctr];
				        blogNames[ctr] = contentHash.get("blogName").toString();
				        urls[ctr] = contentHash.get("xmlrpc").toString(); 					        
				        blogIds[ctr] = Integer.parseInt(contentHash.get("blogid").toString());

				    String blogName = blogNames[ctr];
				    String blogURL = urls[ctr];
				    int blogId = blogIds[ctr];
				    
				    ctr++;
				    
	                WordPressDB settingsDB = new WordPressDB(addAccount.this);
	                
	                //check if this blog is already set up
	                boolean noMatch = false;
	                noMatch = settingsDB.checkMatch(addAccount.this, blogName, blogURL, username);
	                
	                if (noMatch){
	                	Thread prompt = new Thread(){
	                	public void run() 
	      			  	{
	                	 Toast.makeText(addAccount.this, getResources().getText(R.string.account_already_exists), Toast.LENGTH_SHORT).show();
	      			  	} 
						}; 
						runOnUiThread(prompt);
			             
	                }
	                else{
	                boolean success = false;
	                //default to 500 pixel image, centered above text with no full size upload
	                if (blogName == ""){
	                	blogName = "(No Blog Title)";
	                }
	                
	                boolean wpcomFlag = false;
	                //check for wordpress.com
	                if (blogURL.toLowerCase().contains("wordpress.com")){
	                	wpcomFlag = true;
	                }
	                
	                //attempt to get the software version
	                HashMap hPost = new HashMap();
	                hPost.put("software_version", "software_version");
	                Object[] vParams = {
	                		1,
	                		username,
	                		password,
	                		hPost
	                };
	                Object versionResult = new Object();
	                try {
						versionResult = (Object) client.call("wp.getOptions", vParams);
					} catch (XMLRPCException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					
					String wpVersion = "";
					if (versionResult != null){
						try {
							contentHash = (HashMap) versionResult;
							HashMap sv = (HashMap) contentHash.get("software_version");
							wpVersion = sv.get("value").toString();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
	                
	                success = settingsDB.addAccount(addAccount.this, blogURL, blogName, username, password, "Above Text", true, false, "500", 5, false, blogId, wpcomFlag, wpVersion);
	                
	                }
	                
			} //end loop
				    pd.dismiss();   
				    if (result.length == 0){
				    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(addAccount.this);
						  dialogBuilder.setTitle("No Blogs Found");
			              dialogBuilder.setMessage("No blogs were found for that account.");
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
				    }
				    else{
				    Bundle bundle = new Bundle();
	                
	                bundle.putString("returnStatus", "SAVE");
	                Intent mIntent = new Intent();
	                mIntent.putExtras(bundle);
	                setResult(RESULT_OK, mIntent);
	                finish();
				    }
		        
			}
        });
        Object[] params = {
        		username,
        		password
        };
        
        
        method.call(params);
        }	
	}

	@Override public boolean onKeyDown(int i, KeyEvent event) {

		  // only intercept back button press
		  if (i == KeyEvent.KEYCODE_BACK) {
         	 Bundle bundle = new Bundle();
             
             bundle.putString("returnStatus", "CANCEL");
             Intent mIntent = new Intent();
             mIntent.putExtras(bundle);
             setResult(RESULT_OK, mIntent);
             finish();
		  }

		  return false; // propagate this keyevent
		}
	
	interface XMLRPCMethodCallback {
		void callFinished(Object[] result);
	}

	class XMLRPCMethod extends Thread {
		private String method;
		private Object[] params;
		private Handler handler;
		private XMLRPCMethodCallback callBack;
		public XMLRPCMethod(String method, XMLRPCMethodCallback callBack) {
			this.method = method;
			this.callBack = callBack;

			handler = new Handler();
					
		}
		public void call() {
			call(null);
		}
		public void call(Object[] params) {
			this.params = params;
			start();
		}
		@Override
		public void run() {
			try {
				final long t0 = System.currentTimeMillis();
				final Object[] result;
				result = (Object[]) client.call(method, params);
				final long t1 = System.currentTimeMillis();
				handler.post(new Runnable() {
					public void run() {
						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						e.printStackTrace();
						pd.dismiss();
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(addAccount.this);
						  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
						  String message = e.getMessage();
						  if (message.equals("HTTP status code: 404 != 200")){
							  message = "xmlrpc.php not found, please check your path";
						  }
			              dialogBuilder.setMessage(message);
			              dialogBuilder.setPositiveButton("OK",  new
			            		  DialogInterface.OnClickListener() {
	                          public void onClick(DialogInterface dialog, int whichButton) {
	                              // Just close the window.
	                      
	                          }
	                      });
			              dialogBuilder.setCancelable(true);
			             dialogBuilder.create().show();
					}
				});        
					
			} catch (final XMLRPCException e) {
				
				handler.post(new Runnable() {
					public void run() {
						Throwable couse = e.getCause();
						e.printStackTrace();
						pd.dismiss();
						if (couse instanceof HttpHostConnectException) {

						} else {
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(addAccount.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
							  String message = e.getMessage();
							  if (message.equals("HTTP status code: 404 != 200")){
								  message = "xmlrpc.php not found, please check your path";
							  }
				              dialogBuilder.setMessage(message);
				              dialogBuilder.setPositiveButton("OK",  new
				            		  DialogInterface.OnClickListener() {
		                          public void onClick(DialogInterface dialog, int whichButton) {
		                              // Just close the window.
		                      
		                          }
		                      });
				              dialogBuilder.setCancelable(true);
				             dialogBuilder.create().show();
						}
						e.printStackTrace();
						
					}
				});
			}	
		}
		
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    } 
	
	private String getRSDMetaTagHref(String urlString) {
		//get the html code
		
		InputStream in = getResponse(urlString);

		//parse the html and get the attribute for xmlrpc endpoint
		if(in != null) {
			XmlPullParser parser = Xml.newPullParser();
			try {
	            // auto-detect the encoding from the stream
	            parser.setInput(in, null);
	            int eventType = parser.getEventType();
	            boolean done = false;
	            while (eventType != XmlPullParser.END_DOCUMENT){
	                String name = null;
	                String rel="";
					String type="";
					String href="";
	                switch (eventType){
	                    case XmlPullParser.START_TAG:
	                        name = parser.getName();
	                            if (name.equalsIgnoreCase("link")){
	                            	for (int i = 0; i < parser.getAttributeCount(); i++) {
	      							  String attrName = parser.getAttributeName(i);
	      							  String attrValue = parser.getAttributeValue(i);
	      					           if(attrName.equals("rel")){
	      					        	   rel = attrValue;
	      					           }
	      					           else if(attrName.equals("type"))
	      					        	   type = attrValue;
	      					           else if(attrName.equals("href"))
	      					        	   href = attrValue;
	      					           
	      						//	  Log.trace("attribute name: "+ parser.getAttributeName(i));
	      						//	  Log.trace("attribute value: "+parser.getAttributeValue(i));
	      					        }
	      							
	      						  if(rel.equals("EditURI") && type.equals("application/rsd+xml")){
	      							  return href;
	      						  }
	                             //   currentMessage.setLink(parser.nextText());
	                            }                          
	                        break;
	                }
	                eventType = parser.next();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }

		}
		return null;  //never found the rsd tag
}
	
	private String getXMLRPCUrl(String urlString) {
		//get the html code
		
		InputStream in = getResponse(urlString);

		//parse the html and get the attribute for xmlrpc endpoint
		if(in != null) {
			//try {		

			XmlPullParser parser = Xml.newPullParser();
			try {
	            // auto-detect the encoding from the stream
	            parser.setInput(in, null);
	            int eventType = parser.getEventType();
	            boolean done = false;
	            while (eventType != XmlPullParser.END_DOCUMENT){
	            	String name="";
					String apiLink="";
	                switch (eventType){
	                    case XmlPullParser.START_TAG:
	                        name = parser.getName();
	                            if (name.equalsIgnoreCase("api")){
	                            	for (int i = 0; i < parser.getAttributeCount(); i++) {
	      							  String attrName = parser.getAttributeName(i);
	      							  String attrValue = parser.getAttributeValue(i);
	      					           if(attrName.equals("name")){
	      					        	   name = attrValue;
	      					           }
	      					           else if(attrName.equals("apiLink")){
	      					        	   apiLink = attrValue;
	      					           }

	      					           
	      						//	  Log.trace("attribute name: "+ parser.getAttributeName(i));
	      						//	  Log.trace("attribute value: "+parser.getAttributeValue(i));
	      					        }
	      							
	                              if(name.equals("WordPress") ){
	      							  return apiLink;
	      						  }
	                             //   currentMessage.setLink(parser.nextText());
	                            }                          
	                        break;
	                }
	                eventType = parser.next();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }
			
			
		
		}
		return null;  //never found the rsd tag
}

	private InputStream getResponse(String urlString) {
		// TODO Auto-generated method stub
		InputStream in = null;
		int response = -1;
        
        URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} 
        URLConnection conn = null;
		try {
			conn = url.openConnection();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
        
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.addRequestProperty("user-agent", "Mozilla/5.0");
            httpConn.connect(); 

            response = httpConn.getResponseCode();                 
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();                                 
            }                     
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            return null;           
        } 
		return in;
	}
}