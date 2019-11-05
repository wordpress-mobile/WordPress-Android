package org.wordpress.android.util;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.NaCl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class EncryptionUtils {
    static final int BOX_SEALBYTES = NaCl.sodium().crypto_box_sealbytes();
    static final int XCHACHA20POLY1305_KEYBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_keybytes();
    static final int XCHACHA20POLY1305_STATEBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_statebytes();
    static final int XCHACHA20POLY1305_HEADERBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_headerbytes();
    static final int XCHACHA20POLY1305_ABYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_abytes();
    static final int BASE64_ENCODE_FLAGS = Base64.DEFAULT;

    static final String PUBLIC_KEY_BASE64 = "K0y2oQ++gEN00S4CbCH3IYoBIxVF6H86Wz4wi2t2C3M=";
    static final String KEYED_WITH = "v1";

    /**
     * returns a JSON String containing following data:
     * {
     * "keyedWith": "v1",
     * "encryptedKey": "$key_as_base_64",  // The encrypted AES key
     * "header": "base_64_encoded_header", // The xchacha20poly1305 stream header
     * "messages": []                      // the stream elements, base-64 encoded
     *}
     */
    public static String getEncryptedAppLog(Context context) throws JSONException, UnsupportedEncodingException {
        JSONObject encryptedLogJson = new JSONObject();
        encryptedLogJson.put("keyedWith", KEYED_WITH);

        // Shared secret key required to encrypt the stream
        byte[] key = new byte[XCHACHA20POLY1305_KEYBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_keygen(key);

        // Encrypt, encode and add key to JSON object

        byte[] encryptedKey = new byte[XCHACHA20POLY1305_KEYBYTES + BOX_SEALBYTES];
        byte[] publicKeyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT);
        NaCl.sodium().crypto_box_seal(encryptedKey, key, XCHACHA20POLY1305_KEYBYTES, publicKeyBytes);
        encryptedLogJson.put("encryptedKey", Base64.encodeToString(encryptedKey, BASE64_ENCODE_FLAGS));

        // Set up a new stream: initialize the state and create the header
        byte[] state = new byte[XCHACHA20POLY1305_STATEBYTES];
        byte[] header = new byte[XCHACHA20POLY1305_HEADERBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_init_push(state, header, key);

        encryptedLogJson.put("header", Base64.encodeToString(header, BASE64_ENCODE_FLAGS));

        // encrypt the logs and add to JSON array
        String logText = AppLog.toPlainText(context);
        String[] logLines = logText.split("\n");
        JSONArray encryptedLogLinesJson = new JSONArray();
        int[] clen = new int[0];
        byte[] ad = new byte[0];

        for (String logLine : logLines) {
            // add the line break back to the log line
            logLine = logLine + "\n";
            byte[] encryptedLogLine = new byte[logLine.length() + XCHACHA20POLY1305_ABYTES];

            NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
                state,
                encryptedLogLine,
                clen,
                logLine.getBytes(),
                logLine.length(),
                ad,
                0,
                (short) 0);

            encryptedLogLinesJson.put(Base64.encodeToString(encryptedLogLine, BASE64_ENCODE_FLAGS));
        }

        // last element in the JSON array is an encrypted and encoded empty string with FINAL tag
        String emptyString = new String();
        byte[] encryptedLogLine = new byte[XCHACHA20POLY1305_ABYTES];

        NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
            state,
            encryptedLogLine,
            clen,
            emptyString.getBytes(),
            0,
            ad,
            0,
            (short) NaCl.sodium().crypto_secretstream_xchacha20poly1305_tag_final());

        encryptedLogLinesJson.put(Base64.encodeToString(encryptedLogLine, BASE64_ENCODE_FLAGS));

        encryptedLogJson.put("messages", encryptedLogLinesJson);

        // ******* test code begin
        // write test output files
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

            BufferedWriter logOutput = new BufferedWriter(new FileWriter(path + "app_log.txt"));
            logOutput.write(logText);
            logOutput.close();

            // write encrypted text output file
            BufferedWriter encryptedLogOut = new BufferedWriter(new FileWriter(path + "app_log_encrypted.json"));
            encryptedLogOut.write(encryptedLogJson.toString(4));
            encryptedLogOut.close();

            ToastUtils.showToast(context, "EncryptionUtils test code, test files saved to: " + path);
        } catch (IOException e) {
            ToastUtils.showToast(context, "EncryptionUtils test code, IOException: " + e.toString());
        }
        // ******* test code end

        return encryptedLogJson.toString();
    }
}

