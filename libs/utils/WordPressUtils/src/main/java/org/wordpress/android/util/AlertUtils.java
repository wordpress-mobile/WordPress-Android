/*
 * Copyright (C) 2011 wordpress.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wordpress.android.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertUtils {
    /**
     * Show Alert Dialog
     * @param context
     * @param titleId
     * @param messageId
     */
    public static void showAlert(Context context, int titleId, int messageId) {
        Dialog dlg = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(messageId)
                .create();

        dlg.show();
    }

    /**
     * Show Alert Dialog
     * @param context
     * @param titleId
     * @param message
     */
    public static void showAlert(Context context, int titleId, String message) {
        Dialog dlg = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(message)
                .create();

        dlg.show();
    }

    /**
     * Show Alert Dialog
     * @param context
     * @param titleId
     * @param messageId
     * @param positiveButtontxt
     * @param positiveListener
     * @param negativeButtontxt
     * @param negativeListener
     */
    public static void showAlert(Context context, int titleId, int messageId,
                                 CharSequence positiveButtontxt, DialogInterface.OnClickListener positiveListener,
                                 CharSequence negativeButtontxt, DialogInterface.OnClickListener negativeListener) {
        Dialog dlg = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setPositiveButton(positiveButtontxt, positiveListener)
                .setNegativeButton(negativeButtontxt, negativeListener)
                .setMessage(messageId)
                .setCancelable(false)
                .create();

        dlg.show();
    }

    /**
     * Show Alert Dialog
     * @param context
     * @param titleId
     * @param message
     * @param positiveButtontxt
     * @param positiveListener
     */
    public static void showAlert(Context context, int titleId, String message,
                                 CharSequence positiveButtontxt, DialogInterface.OnClickListener positiveListener) {
        Dialog dlg = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setPositiveButton(positiveButtontxt, positiveListener)
                .setMessage(message)
                .setCancelable(false)
                .create();

        dlg.show();
    }
}