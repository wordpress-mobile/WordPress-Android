package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.R;



public class AddCommentActivity extends Activity {
    String accountName, postID = "", comment;
    int commentID = 0;
    boolean isReply = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_comment);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            accountName = extras.getString("accountName");
            isReply = extras.containsKey("commentID");
            commentID = extras.getInt("commentID");
            postID = extras.getString("postID");
            comment = extras.getString("comment");
        }

        final TextView promptLabel = (TextView) findViewById(R.id.commentLabel);
        final Button cancelButton = (Button) findViewById(R.id.cancel);
        final Button okButton = (Button) findViewById(R.id.ok);
        
        if (isReply) {
            setTitle(getResources().getText(R.string.reply_to_comment));
            promptLabel.setText(getResources().getText(R.string.reply_enter));
            okButton.setText(getResources().getText(R.string.reply_send));
        } else {
            setTitle(getResources().getText(R.string.write_comment));
        }
        
        if (comment != null) {
            EditText commentTextET = (EditText)findViewById(R.id.commentText);
            commentTextET.setText(comment);
        }

        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText commentTextET = (EditText)findViewById(R.id.commentText);
                String commentText = commentTextET.getText().toString();

                if (commentText.equals("")) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddCommentActivity.this);
                    dialogBuilder.setTitle(getResources().getText(R.string.add_comment_required));
                    if (isReply) {
                        dialogBuilder.setMessage(getResources().getText(R.string.reply_please_enter));
                    } else {
                        dialogBuilder.setMessage(getResources().getText(R.string.add_comment_please_enter));
                    }
                    dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                        }
                    });
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();
                } else {
                    Bundle bundle = new Bundle();
    
                    bundle.putString("commentText", commentText);
                    bundle.putString("postID", postID);
                    if (isReply) {
                        bundle.putInt("commentID", commentID);
                    }
    
                    Intent mIntent = new Intent();
                    mIntent.putExtras(bundle);
                    setResult(RESULT_OK, mIntent);
                    finish();
                }

            }
        });

        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                 Bundle bundle = new Bundle();

                 bundle.putString("commentText", "CANCEL");
                 Intent mIntent = new Intent();
                 mIntent.putExtras(bundle);
                 setResult(RESULT_OK, mIntent);
                 finish();
            }
        });

    }


}
