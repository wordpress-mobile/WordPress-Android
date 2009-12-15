//by Dan Roundhill, danroundhill.com/wptogo

package org.wordpress.android;
import java.util.HashMap;
import org.apache.http.conn.HttpHostConnectException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



public class newAccount extends Activity {
	private XMLRPCClient client;
	public boolean success = false;
	public ProgressDialog pd;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.newaccount);
		
		Spinner spinner = (Spinner)this.findViewById(R.id.maxImageWidth);
		ArrayAdapter spinnerArrayAdapter = new ArrayAdapter<Object>(this,
	        R.layout.spinner_textview,
	            new String[] { "Original Size", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"});
	    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerArrayAdapter);
	    
		TextView eulaTV = (TextView) this.findViewById(R.id.l_EULA);
		
		eulaTV.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
    		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newAccount.this);
  			  dialogBuilder.setTitle("End User License Agreement");
              dialogBuilder.setMessage(R.string.EULA);
              dialogBuilder.setPositiveButton("OK",  new
            		  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // just close the window.
    
                    }
                });
              dialogBuilder.setCancelable(true);
             dialogBuilder.create().show();
            }
        });  
        
        final customButton cancelButton = (customButton) findViewById(R.id.cancel);
        final customButton saveButton = (customButton) findViewById(R.id.save);
        
        saveButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	Thread action = new Thread() 
				{ 
				  public void run() 
				  {
					  pd = ProgressDialog.show(newAccount.this,
				                "Account Setup", "Attempting to configure account", true, false);
				  } 
				}; 
				runOnUiThread(action);
                
                //capture the entered fields *needs validation*
                EditText urlET = (EditText)findViewById(R.id.url);
                String blogURL = urlET.getText().toString();
                EditText usernameET = (EditText)findViewById(R.id.username);
                final String username = usernameET.getText().toString();
                EditText passwordET = (EditText)findViewById(R.id.password);
                final String password = passwordET.getText().toString();
                
                
                RadioGroup imageRG = (RadioGroup)findViewById(R.id.imagePlacement);
                RadioButton checkedRB = (RadioButton)findViewById(imageRG.getCheckedRadioButtonId());
                final String buttonValue = checkedRB.getText().toString();
                final boolean fullSizeImageValue = false;
                
                Spinner spinner = (Spinner)findViewById(R.id.maxImageWidth);
                final String maxImageWidth = spinner.getSelectedItem().toString();
                long maxImageWidthId = spinner.getSelectedItemId();
                final int maxImageWidthIdInt = (int) maxImageWidthId;
                CheckBox centerThumbnail = (CheckBox)findViewById(R.id.centerThumbnail);
                final boolean centerThumbnailValue = centerThumbnail.isChecked();
                //add http  or http to the beginning of the URL if needed
                if (!(blogURL.toLowerCase().contains("http://")) && !(blogURL.toLowerCase().contains("https://"))){
                	blogURL = "http://" + blogURL;  //default to http
                }
                
                String lastChar = blogURL.substring(blogURL.length() - 1, blogURL.length());

                if (lastChar.equals("/")){
                	blogURL = blogURL.substring(0, blogURL.length() - 1);
                }
                
                final String fBlogURL = blogURL + "/xmlrpc.php";
                
                //verify settings
                client = new XMLRPCClient(fBlogURL);
            	
            	XMLRPCMethod method = new XMLRPCMethod("wp.getUsersBlogs", new XMLRPCMethodCallback() {
    				public void callFinished(Object[] result) {
    					String s = "done";
    					s = result.toString();
    					
    					pd.dismiss();
    					String[] blogNames = new String[100];
    					String[] urls = new String[100];
    					
    					HashMap contentHash = new HashMap();
    					    
    					int ctr = 0;
    					
    					//loop this!
    					    for (Object item : result){
    					        contentHash = (HashMap) result[ctr];
    					        blogNames[ctr] = contentHash.get("blogName").toString();
    					        urls[ctr] = contentHash.get("xmlrpc").toString(); 					        
    					        

    					    String blogName = blogNames[ctr];
    					    String blogURL = urls[ctr];
    					    
    					    ctr++;
    					    
    		                settingsDB settingsDB = new settingsDB(newAccount.this);
    		                //settingsDB.saveSettings(newAccount.this, blogURL, username, password, buttonValue, centerThumbnailValue, fullSizeImageValue, maxImageWidth, maxImageWidthIdInt);
    		                
    		                //check if this blog is already set up
    		                boolean noMatch = false;
    		                noMatch = settingsDB.checkMatch(newAccount.this, blogName, blogURL, username);
    		                
    		                if (noMatch){
    		                	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newAccount.this);
    							  dialogBuilder.setTitle("Account Already Exists");
    				              dialogBuilder.setMessage("There is already a wpToGo account for this wordpress blog configured.");
    				              dialogBuilder.setPositiveButton("Ok",  new
    				            		  DialogInterface.OnClickListener() {
    		                          public void onClick(DialogInterface dialog, int whichButton) {
    		                        	  Bundle bundle = new Bundle();
    	    	    		                
    	    	    		                bundle.putString("returnStatus", "SAVE");
    	    	    		                Intent mIntent = new Intent();
    	    	    		                mIntent.putExtras(bundle);
    	    	    		                setResult(RESULT_OK, mIntent);
    	    	    		                finish();
    		                      
    		                          }
    		                      });
    				              dialogBuilder.setCancelable(true);
    				             dialogBuilder.create().show();
    				             
    				             
    		                }
    		                else{
    		                boolean success = false;
    		                success = settingsDB.addAccount(newAccount.this, blogURL, blogName, username, password, buttonValue, centerThumbnailValue, fullSizeImageValue, maxImageWidth, maxImageWidthIdInt, false);
    		                
    		                // Don't forget to commit your edits!!!
    		               // editor.commit();
    		                
    		                }
    		                
    				} //end loop
    					    
    					    if (success){
    		                	Toast.makeText(newAccount.this, "Account/Blog added successfully!",
    		                            Toast.LENGTH_SHORT).show();

    		                }
    		                else{
    		               /* 	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newAccount.this);
  							  dialogBuilder.setTitle("Error Creating Account");
  				              dialogBuilder.setMessage("Something bad happened while trying to create your local account.  Please check your settings and try again.");
  				              dialogBuilder.setPositiveButton("Ok",  new
  				            		  DialogInterface.OnClickListener() {
  		                          public void onClick(DialogInterface dialog, int whichButton) {
  		                              // Just close the window.
  		                      
  		                          }
  		                      });
  				              dialogBuilder.setCancelable(true);
  				             dialogBuilder.create().show();*/
    		                }
    			      
    					    Bundle bundle = new Bundle();
    		                
    		                bundle.putString("returnStatus", "SAVE");
    		                Intent mIntent = new Intent();
    		                mIntent.putExtras(bundle);
    		                setResult(RESULT_OK, mIntent);
    		                finish();
    			        
    				}
    	        });
    	        Object[] params = {
    	        		username,
    	        		password
    	        };
    	        
    	        
    	        method.call(params);
                
                
            }
        });   
        
        cancelButton.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	 Bundle bundle = new Bundle();
                 
                 bundle.putString("returnStatus", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });
        
        
        
     
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
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newAccount.this);
						  dialogBuilder.setTitle("Connection Error");
			              dialogBuilder.setMessage(e.getFaultString());
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
							//status.setText("Cannot connect to " + uri.getHost() + "\nMake sure server.py on your development host is running !!!");
						} else {
							AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(newAccount.this);
							  dialogBuilder.setTitle("Connection Error");
				              dialogBuilder.setMessage(e.getMessage() + e.getLocalizedMessage());
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
}
