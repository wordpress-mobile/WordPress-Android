package org.wordpress.android;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class viewComment extends Activity {
	
	private String id = "";
	private String comment= "";
	private String accountName = "";
	private String email = "";
	private String name = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getString("id");
         accountName = extras.getString("accountName");   
         comment = extras.getString("comment");
         email = extras.getString("email");
         name = extras.getString("name");
        } 
        
        final Window w = getWindow();
        w.requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.view_comment);
        
        final String gravatarURL = "http://gravatar.com/avatar/" + getMd5Hash(email.trim()) + "?s=64&d=identicon";

        
        Thread action = new Thread() 
		{ 
		  public void run() 
		  {
			  w.setFeatureDrawable(Window.FEATURE_LEFT_ICON, getDrawable(gravatarURL));
		  } 
		}; 
		this.runOnUiThread(action);
        
        
        this.setTitle(name);
        
        final ImageView ivGravatar = (ImageView) findViewById(R.id.gravatar);

		TextView tvComment = (TextView) findViewById(R.id.comment);
		
		tvComment.setText(comment);

	}
	
	public Drawable getDrawable(String imgUrl) { 
        try { 
                 URL url = new URL(imgUrl); 
                 InputStream is = (InputStream) url.getContent(); 
                 Drawable d = Drawable.createFromStream(is, "src"); 
                return d; 
         } catch (MalformedURLException e) { 
                 e.printStackTrace(); 
                 return null; 
         } catch (IOException e) { 
                 e.printStackTrace(); 
                 return null; 
         } 
 } 
	
	public static String getMd5Hash(String input) {
	    try     {
	            MessageDigest md = MessageDigest.getInstance("MD5");
	            byte[] messageDigest = md.digest(input.getBytes());
	            BigInteger number = new BigInteger(1,messageDigest);
	            String md5 = number.toString(16);
	       
	            while (md5.length() < 32)
	                    md5 = "0" + md5;
	       
	            return md5;
	    } catch(NoSuchAlgorithmException e) {
	            Log.e("MD5", e.getMessage());
	            return null;
	    }
	}
}
