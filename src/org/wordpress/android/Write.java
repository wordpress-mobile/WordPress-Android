package org.wordpress.android;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Vector;

public class Write extends Activity {
    /** Called when the activity is first created. */
	public String categoryErrorMsg = "", accountName = "", SD_CARD_TEMP_DIR = "", categories = "", mediaErrorMsg = "";
	private Vector<Uri> selectedImageIDs = new Vector<Uri>();
	long postID;
    public Boolean localDraft = false, centerThumbnail = false, xmlrpcError = false, isPage = false, isNew = false, 
    isAction = false, isUrl = false, locationActive = false, isLargeScreen = false, isCustomPubDate = false;
    private Blog blog;
    private Post post;
    public String setText;
    public int id;
    int styleStart = -1, cursorLoc = 0, screenDensity = 0;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         id = extras.getInt("id");
         blog = new Blog(id, this);
         accountName = EscapeUtils.unescapeHtml(extras.getString("accountName"));
         postID = extras.getLong("postID");
         localDraft = extras.getBoolean("localDraft", false); 
         isPage = extras.getBoolean("isPage", false);
         isNew = extras.getBoolean("isNew", false);
         setText = extras.getString("setText");
         if (!isNew)
             post = new Post(id, postID, isPage, this);
        }
        
        if (isPage){  
        	setContentView(R.layout.post_fullscreen);        }
        else{
        	setContentView(R.layout.post_fullscreen);
        }
        
        final EditText contentEdit = (EditText) findViewById(R.id.fullscreen_textbox);
        contentEdit.setText(setText);   
        contentEdit.addTextChangedListener(new TextWatcher() { 
            public void afterTextChanged(Editable s) { 
            	if (localDraft || isNew){
                	//add style as the user types if a toggle button is enabled
                	ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
                	ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
                	ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
                	ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
                	ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);
                	int position = Selection.getSelectionStart(contentEdit.getText());
            		if (position < 0){
            			position = 0;
            		}
            		
            		if (position > 0){
            			
            			if (styleStart > position || position > (cursorLoc + 1)){
    						//user changed cursor location, reset
    						if (position - cursorLoc > 1){
    							//user pasted text
    							styleStart = cursorLoc;
    						}
    						else{
    							styleStart = position - 1;
    						}
    					}
            			
	                	if (boldButton.isChecked()){  
	                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);

	                		for (int i = 0; i < ss.length; i++) {
	                			if (ss[i].getStyle() == android.graphics.Typeface.BOLD){
	                				s.removeSpan(ss[i]);
	                			}
	                        }
	                		s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                	}
	                	if (emButton.isChecked()){
	                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);
	                		
	                		for (int i = 0; i < ss.length; i++) {
	                			if (ss[i].getStyle() == android.graphics.Typeface.ITALIC){
	                				s.removeSpan(ss[i]);
	                			}
	                        }
	                		s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                	}
	                	if (bquoteButton.isChecked()){
	                		
	                		QuoteSpan[] ss = s.getSpans(styleStart, position, QuoteSpan.class);

	                		for (int i = 0; i < ss.length; i++) {
	                				s.removeSpan(ss[i]);
	                        }
	                		s.setSpan(new QuoteSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                	}
	                	if (underlineButton.isChecked()){
	                		UnderlineSpan[] ss = s.getSpans(styleStart, position, UnderlineSpan.class);

	                		for (int i = 0; i < ss.length; i++) {
	                				s.removeSpan(ss[i]);
	                        }
	                		s.setSpan(new UnderlineSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                	}
	                	if (strikeButton.isChecked()){
	                		StrikethroughSpan[] ss = s.getSpans(styleStart, position, StrikethroughSpan.class);

	                		for (int i = 0; i < ss.length; i++) {
	                				s.removeSpan(ss[i]);
	                        }
	                		s.setSpan(new StrikethroughSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                	}
            		}
            		
            		cursorLoc = Selection.getSelectionStart(contentEdit.getText());
            	}
            } 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { 
                    //unused
            } 
            public void onTextChanged(CharSequence s, int start, int before, int count) { 
                    //unused
            } 
            
});

        final ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);   
        
        boldButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
            	formatBtnClick(boldButton, "strong");
            }
    });

        final Button linkButton = (Button) findViewById(R.id.link);   
        
        linkButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	TextView contentText = (TextView) findViewById(R.id.fullscreen_textbox);

            	int selectionStart = contentText.getSelectionStart();
            	
            	styleStart = selectionStart;
            	
            	int selectionEnd = contentText.getSelectionEnd();
            	
            	if (selectionStart > selectionEnd){
            		int temp = selectionEnd;
            		selectionEnd = selectionStart;
            		selectionStart = temp;
            	}
            	
            	if (selectionStart == -1 || selectionStart == contentText.getText().toString().length() || (selectionStart == selectionEnd)){
            		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Write.this);
            		dialogBuilder.setTitle(getResources().getText(R.string.no_text_selected));
                    dialogBuilder.setMessage(getResources().getText(R.string.select_text_to_link) + " " + getResources().getText(R.string.howto_select_text));
                  dialogBuilder.setPositiveButton("OK",  new
                		  DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        	
                    
                        }
                    });
                  dialogBuilder.setCancelable(true);
                 dialogBuilder.create().show();
            	}
            	else
            	{
            		Intent i = new Intent(Write.this, Link.class);

                	startActivityForResult(i, 2);
            	}    	
           }
        });
        
        
        final ToggleButton emButton = (ToggleButton) findViewById(R.id.em);   
        
        emButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
            	formatBtnClick(emButton, "em");
            }
    });
        
        final ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);   
        
        underlineButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
            	formatBtnClick(underlineButton, "u");
            }
    });
        
        final ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);   
        
        strikeButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
            	formatBtnClick(strikeButton, "strike");
            }
    });
        
        final ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);   
        
        bquoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	formatBtnClick(bquoteButton, "blockquote");

            }
    });
        
        }
    
    
    protected void formatBtnClick(ToggleButton toggleButton, String tag) {
		EditText contentText = (EditText) findViewById(R.id.fullscreen_textbox);

    	int selectionStart = contentText.getSelectionStart();
    	
    	String startTag = "<" + tag + ">";
    	String endTag = "</" + tag + ">";
    	
    	styleStart = selectionStart;
    	
    	int selectionEnd = contentText.getSelectionEnd();
    	
    	if (selectionStart > selectionEnd){
    		int temp = selectionEnd;
    		selectionEnd = selectionStart;
    		selectionStart = temp;
    	}
    	
    	if (localDraft || isNew){             	
        	if (selectionEnd > selectionStart)
        	{
        		Spannable str = contentText.getText();
        		if (tag.equals("blockquote")){
        			

            		QuoteSpan[] ss = str.getSpans(selectionStart, selectionEnd, QuoteSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new QuoteSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("strong")){
        			StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				int style = ((StyleSpan) ss[i]).getStyle();
            				if (style == android.graphics.Typeface.BOLD)
            				{
            					str.removeSpan(ss[i]);
            					exists = true;
            				}
                    }
            		
            		if (!exists){
    					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("em")){
        			StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				int style = ((StyleSpan) ss[i]).getStyle();
            				if (style == android.graphics.Typeface.ITALIC)
            				{
            					str.removeSpan(ss[i]);
            					exists = true;
            				}
                    }
            		
            		if (!exists){
    					str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("u")){
        			

            		UnderlineSpan[] ss = str.getSpans(selectionStart, selectionEnd, UnderlineSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        		else if (tag.equals("strike")){
        			

            		StrikethroughSpan[] ss = str.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class);
        			
        			boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		toggleButton.setChecked(false);
        		}
        	}
    	}
    	else{
    		String content = contentText.getText().toString();
    		if (selectionEnd > selectionStart)
        	{                		
        		contentText.setText(content.substring(0, selectionStart) + startTag + content.substring(selectionStart, selectionEnd) + endTag + content.substring(selectionEnd, content.length()));
        		
        		toggleButton.setChecked(false);
        		contentText.setSelection(selectionStart + content.substring(selectionStart, selectionEnd).length() + startTag.length() + endTag.length());
        	}
        	else if (toggleButton.isChecked()){
        		contentText.setText(content.substring(0, selectionStart) + startTag + content.substring(selectionStart, content.length()));
        		contentText.setSelection(selectionEnd + startTag.length());
        	}
        	else if (!toggleButton.isChecked()){
        		contentText.setText(content.substring(0, selectionStart) + endTag + content.substring(selectionStart, content.length()));
        		contentText.setSelection(selectionEnd + endTag.length());
        	}
    	}
		
	}
    
    //Add settings to menu
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		super.onCreateOptionsMenu(menu);
 		menu.add(0, 0, 0, getResources().getText(R.string.save_and_exit));
 		MenuItem menuItem1 = menu.findItem(0);
 		menuItem1.setIcon(R.drawable.ic_menu_add);
 		menu.add(0, 1, 0, getResources().getText(R.string.exit_without_saving));
 		MenuItem menuItem2 = menu.findItem(1);
 		menuItem2.setIcon(R.drawable.ic_menu_close_clear_cancel);
 		return true;
 	}
 	
 	//Menu actions
 	@Override
 	public boolean onOptionsItemSelected(final MenuItem item){
 		switch (item.getItemId()) {
 		case 0:
 			EditText contentET = (EditText)findViewById(R.id.fullscreen_textbox);
 	        String content = EscapeUtils.unescapeHtml(Html.toHtml(contentET.getText()));
 	        content = content.replace("<p><p>", "<p>");
 	        content = content.replace("</p></p>", "</p>");
 	        content = content.replace("<br><br>", "<br>");
 			Intent i = new Intent(Write.this, EditPost.class);
 			i.putExtra("accountName", accountName);
 			i.putExtra("setText", content);
			i.putExtra("id", id);
			i.putExtra("isNew", true);
 			startActivity(i);
 			return true;
 		case 1:
 			Write.this.finish();
 			return true;
 		}
 		return false;
 	}
 	
 	@Override
 	public void onBackPressed() {
 		EditText contentET = (EditText)findViewById(R.id.fullscreen_textbox);
	        String content = EscapeUtils.unescapeHtml(Html.toHtml(contentET.getText()));
	        content = content.replace("<p><p>", "<p>");
	        content = content.replace("</p></p>", "</p>");
	        content = content.replace("<br><br>", "<br>");
			Intent i = new Intent(Write.this, EditPost.class);
			i.putExtra("accountName", accountName);
			i.putExtra("setText", content);
			i.putExtra("id", id);
			i.putExtra("isNew", true);
			startActivity(i);
			finish();
 	return;
 	}

}