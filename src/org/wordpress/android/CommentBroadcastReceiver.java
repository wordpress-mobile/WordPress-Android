package org.wordpress.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CommentBroadcastReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		context.stopService(new Intent(context, CommentService.class));
		context.startService(new Intent(context, CommentService.class));
	}

	public void onCancel(Context context, Intent intent) {
		context.stopService(new Intent(context, CommentService.class));
	}
}
