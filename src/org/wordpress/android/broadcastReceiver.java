package org.wordpress.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class broadcastReceiver extends BroadcastReceiver {

	protected static Intent svc;
	
	public void onReceive(Context context, Intent intent) {

	context.stopService(new Intent(context, commentService.class));
	context.startService(new Intent(context, commentService.class));

	}
	
	public void onCancel(Context context, Intent intent) {

		context.stopService(new Intent(context, commentService.class));

	}
}
