package org.wordpress.android;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class UploadDialog implements Runnable {

	private String pi_string;
	private TextView tv;
	private ProgressDialog pd;

	public void run() {
		for (int i = 0; i < 20000; i++){
			//yay
		}
		handler.sendEmptyMessage(0);
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			pd.dismiss();
			tv.setText(pi_string);

		}
	};

}
