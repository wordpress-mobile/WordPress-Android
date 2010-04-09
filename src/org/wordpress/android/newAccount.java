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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;



public class newAccount extends Activity {
	private XMLRPCClient client;
	public boolean success = false;
	public String blogURL, xmlrpcURL;
	public ProgressDialog pd;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.new_account);	
		
		this.setTitle("WordPress");
        
        final Button setupAccountButton = (Button) findViewById(R.id.setupAccount);
        final Button createAccountButton = (Button) findViewById(R.id.createAccount);
        
        setupAccountButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Intent i = new Intent(newAccount.this, addAccount.class);
            	startActivityForResult(i, 0);
 
            }
        });   
        
        createAccountButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Intent signupIntent = new Intent(newAccount.this, signup.class); 
            	startActivity(signupIntent);
            }
        });
        
         
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null)
		{

		Bundle extras = data.getExtras();

		switch(requestCode) {
		case 0:
			String action = extras.getString("returnStatus");
			Bundle bundle = new Bundle();
            
            bundle.putString("returnStatus", action);
            Intent mIntent = new Intent();
            mIntent.putExtras(bundle);
            setResult(RESULT_OK, mIntent);
            finish();
		    break;
		}
	}//end null check

	}
	
}