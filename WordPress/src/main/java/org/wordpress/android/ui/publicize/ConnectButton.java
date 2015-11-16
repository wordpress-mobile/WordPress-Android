package org.wordpress.android.ui.publicize;

import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

import java.util.Arrays;

/**
 * Connect/disconnect/reconnect button used in sharing list
 */
public class ConnectButton extends FrameLayout {

    private ConnectAction mConnectAction = ConnectAction.CONNECT;
    private TextView mTxtConnect;
    private ProgressBar mProgress;
    private boolean mIsProgressShowing;

    public ConnectButton(Context context){
        super(context);
        initView(context);
    }

    public ConnectButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ConnectButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.publicize_connect_button, this);

        mTxtConnect = (TextView) findViewById(R.id.text_connect);
        mProgress = (ProgressBar) findViewById(R.id.progress_connect);

        updateView();
    }

    public void showProgress(boolean show) {
        if (mIsProgressShowing == show) return;

        mIsProgressShowing = show;
        mProgress.setVisibility(mIsProgressShowing ? View.VISIBLE : View.INVISIBLE);
        mTxtConnect.setVisibility(mIsProgressShowing ? View.INVISIBLE : View.VISIBLE);

        setEnabled(!mIsProgressShowing);
        updateView();
    }

    private void updateView() {
        if (mIsProgressShowing) {
            int disabledColor = getContext().getResources().getColor(R.color.grey_lighten_30);
            setBackgroundDrawable(new ColorDrawable(disabledColor));
            return;
        }

        int normalColorResId;
        int pressedColorResId;
        int textColorResId;
        int captionResId;

        switch (mConnectAction) {
            case CONNECT:
                normalColorResId = R.color.blue_medium;
                pressedColorResId = R.color.blue_light;
                textColorResId = R.color.white;
                captionResId = R.string.share_btn_connect;
                break;
            case DISCONNECT:
                normalColorResId = R.color.grey_lighten_20;
                pressedColorResId = R.color.grey_lighten_30;
                textColorResId = R.color.grey_darken_20;
                captionResId = R.string.share_btn_disconnect;
                break;
            case RECONNECT:
                normalColorResId = R.color.orange_jazzy;
                pressedColorResId = R.color.orange_fire;
                textColorResId = R.color.white;
                captionResId = R.string.share_btn_reconnect;
                break;
            default:
                return;
        }

        int normalColor = getContext().getResources().getColor(normalColorResId);
        int pressedColor = getContext().getResources().getColor(pressedColorResId);
        Drawable background;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            background = getRippleDrawable(normalColor, pressedColor);
        } else {
            background = getStateListDrawable(normalColor, pressedColor);
        }
        setBackgroundDrawable(background);

        int textColor = getContext().getResources().getColor(textColorResId);
        mTxtConnect.setTextColor(textColor);
        mTxtConnect.setText(captionResId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addPressedAnim();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable getRippleDrawable(int normalColor, int pressedColor) {
        float[] outerRadii = new float[8];
        Arrays.fill(outerRadii, 3);

        RoundRectShape rc = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable shape = new ShapeDrawable(rc);
        shape.getPaint().setColor(normalColor);
        return new RippleDrawable(
                ColorStateList.valueOf(pressedColor),
                shape,
                shape);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addPressedAnim() {
        // note that this only works if a margin is defined for the button in the layout xml
        setStateListAnimator(AnimatorInflater.loadStateListAnimator(getContext(), R.anim.raise));
    }

    private static StateListDrawable getStateListDrawable(int normalColor, int pressedColor) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
        states.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(pressedColor));
        states.addState(new int[]{android.R.attr.state_activated},new ColorDrawable(pressedColor));
        states.addState(new int[]{}, new ColorDrawable(normalColor));
        return states;
    }

    public ConnectAction getAction() {
        return mConnectAction;
    }

    public void setAction(ConnectAction newAction) {
        if (newAction != null && !newAction.equals(mConnectAction)) {
            mConnectAction = newAction;
            updateView();
        }
    }
}