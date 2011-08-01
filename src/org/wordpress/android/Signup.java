package org.wordpress.android;

import java.util.HashMap;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Signup extends Activity {
	public Activity activity = this;
	public HashMap resultData;
	public TextView blogAddressError; 
	public TextView usernameError;
	public TextView passwordError;
	public TextView emailError;
	public Button signUp;
	
	String curBlogAddress;
	String curUsername;
	String curPassword;
	
	String xmlrpcError = "Error";
	
	ProgressBar pb;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);
		setTitle("WordPress - New Account");
		
		blogAddressError = (TextView) findViewById(R.id.blogAddressError);
		usernameError = (TextView) findViewById(R.id.usernameError);
		passwordError = (TextView) findViewById(R.id.passwordError);
		emailError = (TextView) findViewById(R.id.emailError);
		pb = (ProgressBar) findViewById(R.id.loadingSpinner);
		
		signUp = (Button) findViewById(R.id.signupBtn);
		
		signUp.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	signUpUser();
            	
            }
        });
	}

	protected void signUpUser() {
		
		EditText etBlogAddress = (EditText) findViewById(R.id.blogAddress);
		EditText etUsername = (EditText) findViewById(R.id.username);
		EditText etPassword = (EditText) findViewById(R.id.password);
		EditText etEmail = (EditText) findViewById(R.id.email);
		
		String blogAddress = etBlogAddress.getText().toString();
		String username = etUsername.getText().toString();
		String password = etPassword.getText().toString();
		String email = etEmail.getText().toString();
		
		boolean validForm = true;
		
		if (blogAddress.equals("")){
			blogAddressError.setVisibility(View.VISIBLE);
			blogAddressError.setText(getResources().getText(R.string.blog_address) + " is required.");
			validForm = false;
		}
		if (username.equals("")){
			usernameError.setVisibility(View.VISIBLE);
			usernameError.setText(getResources().getText(R.string.stats_username) + " is required.");
			validForm = false;
		}
		if (password.equals("")){
			passwordError.setVisibility(View.VISIBLE);
			passwordError.setText(getResources().getText(R.string.stats_password) + " is required.");
			validForm = false;
		}
		if (email.equals("")){
			emailError.setVisibility(View.VISIBLE);
			emailError.setText(getResources().getText(R.string.email) + " is required.");
			validForm = false;
		}
		
		if (validForm){
			blogAddressError.setVisibility(View.INVISIBLE);
			usernameError.setVisibility(View.INVISIBLE);
			passwordError.setVisibility(View.INVISIBLE);
			emailError.setVisibility(View.INVISIBLE);
			signUp.setEnabled(false);
			pb.setVisibility(View.VISIBLE);
			
			curBlogAddress = blogAddress;
			curUsername = username;
			curPassword = password;
			
			new registerAccountTask().execute(blogAddress, username, password, email);
		}
		
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  //ignore orientation change
	  super.onConfigurationChanged(newConfig);
	}
	
	private class registerAccountTask extends AsyncTask<String, Void, Boolean> {

	     protected void onPostExecute(Boolean result) {
	    	 pb.setVisibility(View.INVISIBLE);
	    	 signUp.setEnabled(true);
	    	 if (result){
	    		 
	    		 if (resultData.get("user_name") != null){
	    			 usernameError.setVisibility(View.VISIBLE);
	    			 usernameError.setText(resultData.get("user_name").toString());
		    	 }
	    		 if (resultData.get("user_email") != null){
	    			emailError.setVisibility(View.VISIBLE);
	    			emailError.setText(resultData.get("user_email").toString());
	    		 }
	    		 if (resultData.get("pass1") != null){
		    		passwordError.setVisibility(View.VISIBLE);
		    		passwordError.setText(resultData.get("pass1").toString());
		    	 }
	    		 if (resultData.get("blogname") != null){
	    			 blogAddressError.setVisibility(View.VISIBLE);
	    			 blogAddressError.setText(resultData.get("blogname").toString());
		    	 }
	    		 
	    		 boolean success = Boolean.parseBoolean(resultData.get("success").toString());
	    		 
	    		 //check for successful registration
	    		 if (success){
	    			 WordPressDB wpdb = new WordPressDB(Signup.this);
	    			 Toast.makeText(Signup.this, "Please click the activation link in the email we have sent to you, then return here to start blogging!", Toast.LENGTH_LONG).show();
	    			 //wpdb.addAccount(Signup.this, "http://" + curBlogAddress + ".wordpress.com/xmlrpc.php", curBlogAddress + "'s Blog", curUsername, curPassword, "", "", "Above Text", true, false, "500", 5, false, 0, true, "3.0.3", false);
	    			 Bundle bundle = new Bundle();
		                bundle.putString("returnStatus", "SAVE");
		                Intent mIntent = new Intent();
		                mIntent.putExtras(bundle);
		                setResult(RESULT_OK, mIntent);
		                finish();
	    		 }
	    		 
	    	 }
	    	 else{
	    		 
	    	 AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Signup.this);
			  dialogBuilder.setTitle(getResources().getText(R.string.connection_error));
           dialogBuilder.setMessage(xmlrpcError);
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
	     
	     @Override
		protected Boolean doInBackground(String... args) {
	    	 
	    	resultData = new HashMap(); 
	    	 
	    	XMLRPCClient client = new XMLRPCClient("https://wordpress.com/xmlrpc.php", "", "");
	    	
	    	String blogAddress = args[0];
	    	String username = args[1];
	    	String password = args[2];
	    	String email = args[3];
		    
		    Object[] vParams = {
		    		blogAddress,
		    		username,
		    		password,
		    		email
		    };
		    
		    try {
				resultData = (HashMap) client.call("wpcom.registerAccount", vParams);
				
			} catch (XMLRPCException e) {
				xmlrpcError = e.getMessage();
				return false;
			}
			
			if (resultData.size() > 0){
				return true;
			}
	    	
			return false;
			
		}

	 }

}
