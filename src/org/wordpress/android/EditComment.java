package org.wordpress.android;

import java.util.HashMap;

import org.wordpress.android.models.Comment;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class EditComment extends Activity {
	
	private int ID_DIALOG_SAVING = 0;
	public Boolean isLargeScreen = false;
	public ProgressDialog pd;
	public String xmlErrorMessage;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// need to make sure we have db and currentBlog on views that don't use
		// the Action Bar
		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		if (height > width) {
			width = height;
		}
		if (width > 480) {
			isLargeScreen = true;
		}
		
		setContentView(R.layout.edit_comment);
		
		// Retrieve a reference to the current comment.
		Comment comment = WordPress.currentComment;
		
		final EditText authorNameET = (EditText) this.findViewById(R.id.author_name);
		authorNameET.setText(comment.name);
		
		final EditText authorEmailET = (EditText) this.findViewById(R.id.author_email);
		authorEmailET.setText(comment.authorEmail);
		
		final EditText authorURLET = (EditText) this.findViewById(R.id.author_url);
		authorURLET.setText(comment.authorURL);
		
		final EditText commentContentET = (EditText) this.findViewById(R.id.comment_content);
		commentContentET.setText(comment.comment);
		
		String[] items = new String[] {
				getResources().getString(R.string.approved),
				getResources().getString(R.string.pending_review),
				getResources().getString(R.string.spam) };
		Spinner spinner = (Spinner) findViewById(R.id.status);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		String status = comment.status;
		
		if (status.equals("approve")) {
			spinner.setSelection(0, true);
		} else if (status.equals("hold")) {
			spinner.setSelection(1, true);
		} else if (status.equals("spam")) {
			spinner.setSelection(2, true);
		}
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
				// do nothing
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});
		
		final Button saveButton = (Button) findViewById(R.id.post);
		saveButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				boolean result = saveComment();
				if (result) {
					finish();
				}
			}
		});

	}


	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_SAVING) {
			ProgressDialog savingDialog = new ProgressDialog(this);
			savingDialog.setMessage(getResources().getText(R.string.saving_changes));
			savingDialog.setIndeterminate(true);
			savingDialog.setCancelable(true);
			return savingDialog;
		}
		return super.onCreateDialog(id);
	}

	public boolean saveComment() {

		// grab the form data
		EditText authorNameET = (EditText) findViewById(R.id.author_name);
		String authorName = authorNameET.getText().toString();
		
		EditText authorEmailET = (EditText) findViewById(R.id.author_email);
		String authorEmail = authorEmailET.getText().toString();
		
		EditText authorURLET = (EditText) findViewById(R.id.author_url);
		String authorURL = authorURLET.getText().toString();
		
		EditText contentET = (EditText) findViewById(R.id.comment_content);
		String content = contentET.getText().toString();
		
		Spinner spinner = (Spinner) findViewById(R.id.status);
		int selectedStatus = spinner.getSelectedItemPosition();
		String status = "";
		switch (selectedStatus) {
		case 0:
			status = "approve";
			break;
		case 1:
			status = "hold";
			break;
		case 2:
			status = "spam";
			break;
		}
		
		// Sanity check the edited fields before we save.
		CharSequence dialogMsg = "";
		if (content.equals("")) {
			dialogMsg = getResources().getText(R.string.content_required);
//		} else if(authorName.equals("")) {
//			dialogMsg = getResources().getText(R.string.author_name_required);
//		} else if(authorEmail.equals("")) {
//			dialogMsg = getResources().getText(R.string.author_email_required);
		}
		if (!dialogMsg.equals("")) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditComment.this);
			dialogBuilder.setTitle(getResources()
						 .getText(R.string.required_field));
			dialogBuilder.setMessage(dialogMsg);
			dialogBuilder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Just close the window
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
			
			// Short circuit and let the user address the issue.
			return false;
		}
		
		// If nothing has been changed, skip the rest and just return.
		Comment comment = WordPress.currentComment;
		if(authorName.equals(comment.name) && 
				authorEmail.equals(comment.authorEmail) &&
				authorURL.equals(comment.authorURL) &&
				content.equals(comment.comment)) {
			return true;
		}
		
		final HashMap<String, String> postHash = new HashMap<String, String>();
		postHash.put("status", status);
		postHash.put("content", content);
		postHash.put("author", authorName);
		postHash.put("author_url", authorURL);
		postHash.put("author_email", authorEmail);
		
		// show loading message, then do the work
		showDialog(ID_DIALOG_SAVING);
		
		new Thread() {
			public void run() {
				updateComment(postHash);
			}
		}.start();
		
		return false;
	}
	
	
	public void updateComment(HashMap<String, String> postHash){
		
		// Update the comment on the user's blog.
		XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
				WordPress.currentBlog.getHttpuser(),
				WordPress.currentBlog.getHttppassword());

		Object[] params = { WordPress.currentBlog.getBlogId(),
				WordPress.currentBlog.getUsername(),
				WordPress.currentBlog.getPassword(), 
				WordPress.currentComment.commentID,
				postHash };

		xmlErrorMessage = "";
		Object result = null;
		try {
			result = (Object) client.call("wp.editComment", params);
			boolean bResult = Boolean.parseBoolean(result.toString());
			if (bResult) {
				
				// Our database expects different values in the hash than the xmlrpc request.
				// Make the necessary adjustments, and clean up the old keys.
				postHash.put("url", postHash.get("author_url"));
				postHash.put("email", postHash.get("author_email"));
				postHash.put("comment", postHash.get("content"));
				
				postHash.remove("author_url");
				postHash.remove("author_email");
				postHash.remove("content");
				
				// Save the updates 
				WordPress.wpDB.updateComment(
						WordPress.currentBlog.getId(),
						WordPress.currentComment.commentID,
						postHash);
				
				// Everything was saved successfully, so now we can update the
				// current comment. 
				WordPress.currentComment.authorEmail = postHash.get("email");
				WordPress.currentComment.authorURL = postHash.get("url");
				WordPress.currentComment.comment = postHash.get("comment");
				WordPress.currentComment.status = postHash.get("status");
				WordPress.currentComment.name = postHash.get("author");
			}
		} catch (XMLRPCException e) {
			xmlErrorMessage = e.getLocalizedMessage();
		}
		dismissDialog(ID_DIALOG_SAVING);
		
		if(xmlErrorMessage != "") {

			Thread dialogThread = new Thread() {
				public void run() {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditComment.this);
					dialogBuilder.setTitle(getResources().getText(R.string.error));
					dialogBuilder.setMessage(xmlErrorMessage);
					dialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
					      public void onClick(DialogInterface dialog, int whichButton) {
					          // Just close the window.
					  
					      }
					  });
					 dialogBuilder.setCancelable(true);
					 dialogBuilder.create().show();					
				}
			};
			runOnUiThread(dialogThread);
		} else {
			finish();
		}
		
	}
	
	
	@Override
	public boolean onKeyDown(int i, KeyEvent event) {

		// only intercept back button press
		if (i == KeyEvent.KEYCODE_BACK) {

			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					EditComment.this);
			dialogBuilder.setTitle(getResources().getText(R.string.cancel_edit));
			dialogBuilder.setMessage(getResources().getText(R.string.sure_to_cancel_edit_comment));
			dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Bundle bundle = new Bundle();

							bundle.putString("returnStatus", "CANCEL");
							Intent mIntent = new Intent();
							mIntent.putExtras(bundle);
							setResult(RESULT_OK, mIntent);
							finish();
						}
					});
			dialogBuilder.setNegativeButton(
					getResources().getText(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// just close the dialog window
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		}

		return false;
	}

}
