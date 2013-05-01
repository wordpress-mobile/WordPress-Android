package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;

import com.loopj.android.http.BinaryHttpResponseHandler;

public abstract class BitmapResponseHandler extends BinaryHttpResponseHandler {
    public BitmapResponseHandler(){
        super(new String[]{ "image/jpeg", "image/gif", "image/png"});
    };
    public abstract void onSuccess(int statusCode, Bitmap bitmap);
    protected void handleSuccessMessage(int statusCode, Bitmap bitmap){
        onSuccess(statusCode, bitmap);
    }
    @Override
    protected void sendSuccessMessage(int statusCode, byte[] responseBody) {
        // turn this beast into a bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
        if (bitmap == null) {
            super.sendSuccessMessage(statusCode, responseBody);
        } else {
            sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, bitmap}));
        }
        // sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, responseBody}));
    }
    // Methods which emulate android's Handler and Message methods
    @Override
    protected void handleMessage(Message msg) {
        Object[] response;
        switch(msg.what) {
            case SUCCESS_MESSAGE:
                response = (Object[])msg.obj;
                handleSuccessMessage(((Integer) response[0]).intValue() , (Bitmap) response[1]);
                break;
            case FAILURE_MESSAGE:
                response = (Object[])msg.obj;
                handleFailureMessage((Throwable)response[0], response[1].toString());
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }
}