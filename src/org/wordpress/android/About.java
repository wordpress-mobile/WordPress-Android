package org.wordpress.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class About extends Activity{
	final String tos_url = "http://en.wordpress.com/tos";
	final String privacy_policy_url = "http://automattic.com/privacy";
	
	
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		setContentView(R.layout.about);
		
		Button tos = (Button) findViewById(R.id.about_tos);
		tos.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Uri uri = Uri.parse(tos_url);
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		});
		
		Button pp = (Button) findViewById(R.id.about_privacy);
		pp.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Uri uri = Uri.parse(privacy_policy_url);
				startActivity(new Intent(Intent.ACTION_VIEW, uri));				
			}
		});
		
	}
}
