package org.wordpress.android.lockmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.wordpress.android.R;

public class AppUnlockActivity extends Activity {

    private EditText pinCodeField1 = null;
    private EditText pinCodeField2 = null;
    private EditText pinCodeField3 = null;
    private EditText pinCodeField4 = null;
    private InputFilter[] filters = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_unlock);
        
        filters = new InputFilter[2];
        filters[0]= new InputFilter.LengthFilter(1);
        filters[1] = onlyNumber;
        
        //Setup the pin fields row
        pinCodeField1 = (EditText) findViewById(R.id.pincode_1);
        setupPinItem(pinCodeField1);
        
        pinCodeField2 = (EditText) findViewById(R.id.pincode_2);
        setupPinItem(pinCodeField2);
        
        pinCodeField3 = (EditText) findViewById(R.id.pincode_3);
        setupPinItem(pinCodeField3);
        
        pinCodeField4 = (EditText) findViewById(R.id.pincode_4);
        setupPinItem(pinCodeField4);
        
        //setup the keyboard
        ((Button) findViewById(R.id.button0)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button1)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button2)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button3)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button4)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button5)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button6)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button7)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button8)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button9)).setOnClickListener(defaultButtonListener);
        ((Button) findViewById(R.id.button_erase)).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        if( pinCodeField1.isFocused() ) {

                        }
                        else if( pinCodeField2.isFocused() ) {
                            pinCodeField1.requestFocus();
                            pinCodeField1.setText("");
                        }
                        else if( pinCodeField3.isFocused() ) {
                            pinCodeField2.requestFocus();
                            pinCodeField2.setText("");
                        }
                        else if( pinCodeField4.isFocused() ) {
                            pinCodeField3.requestFocus();
                            pinCodeField3.setText("");
                        }
                    }
                });
    }
    
    
    private void setupPinItem(EditText item){
        item.setInputType(InputType.TYPE_NULL); 
        item.setFilters(filters); 
        item.setOnTouchListener(otl);
        item.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }
    
    private OnClickListener defaultButtonListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            int currentValue = -1;
            switch (arg0.getId()) {
                case R.id.button0:
                    currentValue = 0;
                    break;
                case R.id.button1:
                    currentValue = 1;                    
                    break;
                case R.id.button2:
                    currentValue = 2;
                    break;
                case R.id.button3:
                    currentValue = 3;
                    break;
                case R.id.button4:
                    currentValue = 4;
                    break;
                case R.id.button5:
                    currentValue = 5;
                    break;
                case R.id.button6:
                    currentValue = 6;
                    break;
                case R.id.button7:
                    currentValue = 7;
                    break;
                case R.id.button8:
                    currentValue = 8;
                    break;
                case R.id.button9:
                    currentValue = 9;
                    break;
                default:
                    break;
            }
            
            //set the value and move the focus
            String currentValueString = String.valueOf(currentValue);
            if( pinCodeField1.isFocused() ) {
                pinCodeField1.setText(currentValueString);
                pinCodeField2.requestFocus();
                pinCodeField2.setText("");
            }
            else if( pinCodeField2.isFocused() ) {
                pinCodeField2.setText(currentValueString);
                pinCodeField3.requestFocus();
                pinCodeField3.setText("");
            }
            else if( pinCodeField3.isFocused() ) {
                pinCodeField3.setText(currentValueString);
                pinCodeField4.requestFocus();
                pinCodeField4.setText("");
            }
            else if( pinCodeField4.isFocused() ) {
                pinCodeField4.setText(currentValueString);
            }

            if(pinCodeField4.getText().toString().length() > 0 &&
                    pinCodeField3.getText().toString().length() > 0 &&
                    pinCodeField2.getText().toString().length() > 0 &&
                    pinCodeField1.getText().toString().length() > 0
                    ) {
                
                String passLock = pinCodeField1.getText().toString() + pinCodeField2.getText().toString() +
                        pinCodeField3.getText().toString() + pinCodeField4.getText();
                if( AppLockManager.getInstance().getCurrentAppLock().verifyPassword(passLock) ) {
                    finish();
                } else {
                    Thread shake = new Thread() {
                        public void run() {
                            Animation shake = AnimationUtils.loadAnimation(AppUnlockActivity.this, R.anim.shake);
                            findViewById(R.id.AppUnlockLinearLayout1).startAnimation(shake);
                            Toast.makeText(AppUnlockActivity.this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                            pinCodeField1.setText("");
                            pinCodeField2.setText("");
                            pinCodeField3.setText("");
                            pinCodeField4.setText("");
                            pinCodeField1.requestFocus();
                        }
                    };
                    runOnUiThread(shake);
                }
            }
        }
    };


    private InputFilter onlyNumber = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            
            if( source.length() > 1 )
                return "";

            if( source.length() == 0 ) //erase
                return null;

            try {
                int number = Integer.parseInt(source.toString());
                if( ( number >= 0 ) && ( number <= 9 ) )
                    return String.valueOf(number);
                else
                    return "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
    };
    
    private OnTouchListener otl = new OnTouchListener() {
        @Override
        public boolean onTouch (View v, MotionEvent event) {
            if( v instanceof EditText ) {
                ((EditText)v).setText("");
            }
            return false;
        }
    };
    
    @Override
    public void onBackPressed() {
        AppLockManager.getInstance().getCurrentAppLock().forcePasswordLock();
        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        this.startActivity(i);
        finish();
    }
}