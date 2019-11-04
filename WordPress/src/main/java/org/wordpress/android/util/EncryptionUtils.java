package org.wordpress.android.util;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.NaCl;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.AppLog;

public class EncryptionUtils {

    static final int BOX_SEALBYTES = NaCl.sodium().crypto_box_sealbytes();
    static final int XCHACHA20POLY1305_KEYBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_keybytes();
    static final int XCHACHA20POLY1305_STATEBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_statebytes();
    static final int XCHACHA20POLY1305_HEADERBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_headerbytes();
    static final int XCHACHA20POLY1305_ABYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_abytes();

    static final String PUBLIC_KEY = "K0y2oQ++gEN00S4CbCH3IYoBIxVF6H86Wz4wi2t2C3M=";
    static final String KEYED_WITH = "v1";

    /**
     * returns a JSON String with the following:
     * {
     * "keyedWith": "v1",
     * "encryptedKey": "$key_as_base_64",  // The encrypted AES key
     * "header": "base_64_encoded_header", // The xchacha20poly1305 stream header
     * "messages": []                      // the stream elements, base-64 encoded
     *}
     */
    public static String getEncryptedAppLog(Context context) throws JSONException {

        JSONObject encryptedLogJson = new JSONObject();
        encryptedLogJson.put("keyedWith", KEYED_WITH);

        // Shared secret key required to encrypt the stream
        byte[] key = new byte[XCHACHA20POLY1305_KEYBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_keygen(key);

        // Encrypt, encode and add key to JSON object

        byte[] encryptedKey = new byte[XCHACHA20POLY1305_KEYBYTES + BOX_SEALBYTES];
        NaCl.sodium().crypto_box_seal(encryptedKey, key, XCHACHA20POLY1305_KEYBYTES, PUBLIC_KEY.getBytes());
        encryptedLogJson.put("encryptedKey", Base64.encodeToString(encryptedKey, Base64.DEFAULT));

        // Set up a new stream: initialize the state and create the header
        byte[] state = new byte[XCHACHA20POLY1305_STATEBYTES];
        byte[] header = new byte[XCHACHA20POLY1305_HEADERBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_init_push(state, header, key);

        encryptedLogJson.put("header", Base64.encodeToString(header, Base64.DEFAULT));

        // encrypt the logs and add to JSON array
        String logText = AppLog.toPlainText(context);
        String[] logLines = logText.split("\n");
        JSONArray encryptedLogLinesJson = new JSONArray();
        for (String logLine : logLines) {

            byte[] encryptedLogLine = new byte[logLine.length() + XCHACHA20POLY1305_ABYTES];
            byte[] ad = new byte[1];
            int[] clen = new int[1];

            NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
                state,
                encryptedLogLine,
                clen,
                logLine.getBytes(),
                logLine.length(),
                ad,
                0,
                (short) 0);

            encryptedLogLinesJson.put(Base64.encodeToString(encryptedLogLine, Base64.DEFAULT));
        }

        // last element in the JSON array is an encrypted and encoded empty string with FINAL tag
        String emptyString = new String();
        byte[] encryptedLogLine = new byte[XCHACHA20POLY1305_ABYTES];
        byte[] ad = new byte[1];
        int[] clen = new int[1];

        NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
            state,
            encryptedLogLine,
            clen,
            emptyString.getBytes(),
            0,
            ad,
            0,
            (short) NaCl.sodium().crypto_secretstream_xchacha20poly1305_tag_final());

        encryptedLogLinesJson.put(Base64.encodeToString(encryptedLogLine, Base64.DEFAULT));

        encryptedLogJson.put("messages", encryptedLogLinesJson);

        //******* test code begin
        // write test output files
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

            {
                BufferedWriter out = new BufferedWriter(new FileWriter(path + "app_log.txt"));
                out.write(logText);
                out.close();
            }

            // write encrypted text output file
            {
                BufferedWriter out = new BufferedWriter(new FileWriter(path + "app_log_encrypted.json"));
                out.write(encryptedLogJson.toString(4));
                out.close();
            }

            ToastUtils.showToast(context, "EncryptionUtils test code, test files saved to: " + path);
        } catch (IOException e) {
            ToastUtils.showToast(context, "EncryptionUtils test code, IOException: " + e.toString());
        } 
        //******* test code end

        return encryptedLogJson.toString();
    }
}

