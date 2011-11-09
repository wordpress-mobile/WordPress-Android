package org.wordpress.android;

import org.apache.http.conn.HttpHostConnectException;
import org.wordpress.android.util.EscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class AddAccount extends Activity {
	private XMLRPCClient client;
	public boolean success = false;
	public String blogURL, xmlrpcURL;
	public ProgressDialog pd;
	private String httpuser = "";
	private String httppassword = "";
	private boolean wpcom = false;
	private int blogCtr = 0;
	public ArrayList<CharSequence> aBlogNames = new ArrayList<CharSequence>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_account);	
		
		this.setTitle("WordPress - " + getResources().getText(R.string.add_account));
		
		Bundle extras = getIntent().getExtras();
		
        if(extras !=null)
        {
        	wpcom = extras.getBoolean("wpcom", false);
        }
        
        if (wpcom){
        	TextView urlLabel = (TextView) findViewById(R.id.l_url);
        	urlLabel.setVisibility(View.GONE);
        	EditText urlET = (EditText) findViewById(R.id.url);
        	urlET.setVisibility(View.GONE);
        }
        else {
        	ImageView logo = (ImageView) findViewById(R.id.wpcomLogo);
        	logo.setImageDrawable(getResources().getDrawable(R.drawable.wplogo));
        }
        
        final Button settingsButton = (Button) findViewById(R.id.settingsButton);
        if (!wpcom) {
	        settingsButton.setOnClickListener(new Button.OnClickListener() {
	            public void onClick(View v) {
	            	Intent settings = new Intent(AddAccount.this, AddAcountSettings.class);
	            	settings.putExtra("httpuser", httpuser);
	            	settings.putExtra("httppassword", httppassword);
	            	startActivityForResult(settings, R.id.settingsButton);
	            }
	        });
        }
        else {
        	settingsButton.setVisibility(View.GONE);
        }
        

        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button saveButton = (Button) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	

				pd = ProgressDialog.show(AddAccount.this, getResources().getText(R.string.account_setup), getResources().getText(R.string.attempting_configure), true, false);

				
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
            	Intent signupIntent = new Intent(AddAccount.this, Signup.class); 
            	startActivity(signupIntent);
            }
        }); 
	}
	
	protected void configureAccount() {
		
		//capture the entered fields *needs validation*
        EditText urlET = (EditText)findViewById(R.id.url);
        if (wpcom){
        	blogURL = "http://wordpress.com";
        }
        else {
        	blogURL = urlET.getText().toString().trim();
        }
        EditText usernameET = (EditText)findViewById(R.id.username);
        final String username = usernameET.getText().toString().trim();
        EditText passwordET = (EditText)findViewById(R.id.password);
        final String password = passwordET.getText().toString().trim();

        boolean invalidURL = false;
        try {
			URI.create(blogURL);
		} catch (Exception e1) {
			invalidURL = true;
		}
        
        if (blogURL.equals("") || username.equals("") || password.equals("")){
        	pd.dismiss();
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
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
        else if (invalidURL){
        	pd.dismiss();
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.invalid_url));
            dialogBuilder.setMessage(getResources().getText(R.string.invalid_url_message));
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
        client = new XMLRPCClient(fBlogURL, httpuser, httppassword);
    	
    	XMLRPCMethod method = new XMLRPCMethod("wp.getUsersBlogs", new XMLRPCMethodCallback() {
			@SuppressWarnings("unchecked")
			public void callFinished(Object[] result) {
				
				
				final String[] blogNames = new String[result.length];
				final String[] urls = new String[result.length];
				final int[] blogIds = new int[result.length];
				final boolean[] wpcoms = new boolean[result.length];
				final String[] wpVersions = new String[result.length];
				HashMap<Object, Object> contentHash = new HashMap<Object, Object>();
				
				//loop this!
				    for (int ctr = 0; ctr< result.length; ctr++){
				        contentHash = (HashMap<Object, Object>) result[ctr];			        
				        //check if this blog is already set up
		                boolean match = false;
		                String matchBlogName = contentHash.get("blogName").toString();
		                if (matchBlogName.length() == 0){
		                	matchBlogName = contentHash.get("url").toString();
		                } 
		                match = WordPress.wpDB.checkMatch(matchBlogName, contentHash.get("xmlrpc").toString(), username);
		            if (!match){
	                	blogNames[blogCtr] = matchBlogName;			        
				        urls[blogCtr] = contentHash.get("xmlrpc").toString(); 					        
				        blogIds[blogCtr] = Integer.parseInt(contentHash.get("blogid").toString());
					    String blogURL = urls[blogCtr];
		                
		                aBlogNames.add(EscapeUtils.unescapeHtml(blogNames[blogCtr]));
		                
		                boolean wpcomFlag = false;
		                //check for wordpress.com
		                if (blogURL.toLowerCase().contains("wordpress.com")){
		                	wpcomFlag = true;                	
		                }
		                wpcoms[blogCtr] = wpcomFlag;
		                
		                //attempt to get the software version
		                String wpVersion = "";
		                if (!wpcomFlag){
			                HashMap<String, String> hPost = new HashMap<String, String>();
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
							}
							
							if (versionResult != null){
								try {
									contentHash = (HashMap<Object, Object>) versionResult;
									HashMap<?, ?> sv = (HashMap<?, ?>) contentHash.get("software_version");
									wpVersion = sv.get("value").toString();
								} catch (Exception e) {
								}
							}
		                }
		                else{
		                	wpVersion = "3.2";
		                }
						
						wpVersions[blogCtr] = wpVersion;
		                
						blogCtr++;
						
		                //success = settingsDB.addAccount(addAccount.this, blogURL, blogName, username, password, "Above Text", true, false, "500", 5, false, blogId, wpcomFlag, wpVersion);
		                
	                }
			} //end loop
				    pd.dismiss();   
				    if (blogCtr == 0){
				    	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
						  dialogBuilder.setTitle("No Blogs Found");
						  String additionalText = "";
						  if (result.length > 0){
							  additionalText = " additional ";
						  }
			              dialogBuilder.setMessage("No " + additionalText + "blogs were found for that account.");
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
				    	//take them to the blog selection screen if there's more than one blog
				    	if (blogCtr > 1){
				    		
				    		LayoutInflater inflater = (LayoutInflater)AddAccount.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				    		final ListView lv = (ListView) inflater.inflate(R.layout.select_blogs_list, null);
				    		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); 
				            lv.setItemsCanFocus(false);
				            
				            ArrayAdapter<CharSequence> blogs = new ArrayAdapter<CharSequence>(AddAccount.this, R.layout.blogs_row, aBlogNames);
					          
					        lv.setAdapter(blogs);
				    		
				    		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
							  dialogBuilder.setTitle("Select Blogs");
				              dialogBuilder.setView(lv);
				              dialogBuilder.setNegativeButton("Add Selected",  new
				            		  DialogInterface.OnClickListener() {
		                          public void onClick(DialogInterface dialog, int whichButton) {
		                        	SparseBooleanArray selectedItems = lv.getCheckedItemPositions();
		                          	for (int i=0; i<selectedItems.size();i++){
		                          		if (selectedItems.get(selectedItems.keyAt(i)) == true){
		                          			int rowID = selectedItems.keyAt(i);
		                          			success = WordPress.wpDB.addAccount(urls[rowID], blogNames[rowID], username, password, httpuser, httppassword, "Above Text", true, false, "500", 5, false, blogIds[rowID], wpcoms[rowID], wpVersions[rowID]);
		                          		}
		                          	}
		                          	Bundle bundle = new Bundle();
					                bundle.putString("returnStatus", "SAVE");
					                Intent mIntent = new Intent();
					                mIntent.putExtras(bundle);
					                setResult(RESULT_OK, mIntent);
					                finish();
		                      
		                          }
		                      });
				              dialogBuilder.setPositiveButton("Add All",  new
				            		  DialogInterface.OnClickListener() {
		                          public void onClick(DialogInterface dialog, int whichButton) {
		                        	  for (int i=0;i<blogCtr;i++){
		                        		  success = WordPress.wpDB.addAccount(urls[i], blogNames[i], username, password, httpuser, httppassword, "Above Text", true, false, "500", 5, false, blogIds[i], wpcoms[i], wpVersions[i]);
		                        	  }
		                        	  Bundle bundle = new Bundle();
						                bundle.putString("returnStatus", "SAVE");
						                Intent mIntent = new Intent();
						                mIntent.putExtras(bundle);
						                setResult(RESULT_OK, mIntent);
						                finish();
		                          }
		                      });
				              dialogBuilder.setCancelable(true);
				             AlertDialog ad = dialogBuilder.create();
				             ad.setInverseBackgroundForced(true);
				             ad.show();
				             
				             final Button addSelected = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
				             addSelected.setEnabled(false);
				             
				             lv.setOnItemClickListener(new OnItemClickListener() {
									public void onItemClick(AdapterView<?> arg0, View arg1,
											int arg2, long arg3) {
										SparseBooleanArray selectedItems = lv.getCheckedItemPositions();
										boolean isChecked = false;
			                          	for (int i=0; i<selectedItems.size();i++){
			                          		if (selectedItems.get(selectedItems.keyAt(i)) == true){
			                          			isChecked = true;
			                          		}
			                          	}
			                          	if (!isChecked){
			                          		addSelected.setEnabled(false);
			                          	}
			                          	else{
			                          		addSelected.setEnabled(true);
			                          	}	
									}
					            });
				            
				    	}
				    	else {
                  		  	success = WordPress.wpDB.addAccount(urls[0], blogNames[0], username, password, httpuser, httppassword, "Above Text", true, false, "500", 5, false, blogIds[0], wpcoms[0], wpVersions[0]);
						    Bundle bundle = new Bundle();
			                bundle.putString("returnStatus", "SAVE");
			                Intent mIntent = new Intent();
			                mIntent.putExtras(bundle);
			                setResult(RESULT_OK, mIntent);
			                finish();
				    	}
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
	
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // See which child activity is calling us back.
	    switch (requestCode) {
	        case R.id.settingsButton:
	            if (resultCode == RESULT_OK) {
	            	Bundle extras = data.getExtras();
	            	httpuser = extras.getString("httpuser");
	            	httppassword = extras.getString("httppassword");
	            }
	        default:
	            break;
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
				final Object[] result;
				result = (Object[]) client.call(method, params);
				handler.post(new Runnable() {
					public void run() {
						callBack.callFinished(result);
					}
				});
			} catch (final XMLRPCFault e) {
				handler.post(new Runnable() {
					public void run() {
						//e.printStackTrace();
						pd.dismiss();
						String message = e.getMessage();
						  if (message.contains("code 403")){
							  //invalid login
							  Thread shake = new Thread(){
				              public void run()
				              {
				      			  	Animation shake = AnimationUtils.loadAnimation(AddAccount.this, R.anim.shake);
									findViewById(R.id.section1).startAnimation(shake);
									Toast.makeText(AddAccount.this, getResources().getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
				      		  } 
							  }; 
							  runOnUiThread(shake);
						  }
						  else{
							  AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
							  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
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
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddAccount.this);
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
		InputStream in = null;
		int response = -1;
        
        URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		} 
        URLConnection conn = null;
		try {
			conn = url.openConnection();
		} catch (IOException e1) {
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