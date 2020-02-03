package org.wordpress.android.util;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.NaCl;

public class EncryptionUtils {
    public static final int BOX_SEALBYTES = NaCl.sodium().crypto_box_sealbytes();
    public static final int XCHACHA20POLY1305_ABYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_abytes();
    public static final int XCHACHA20POLY1305_KEYBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_keybytes();
    public static final int XCHACHA20POLY1305_STATEBYTES =
            NaCl.sodium().crypto_secretstream_xchacha20poly1305_statebytes();

    public static final short XCHACHA20POLY1305_TAG_FINAL =
            (short) NaCl.sodium().crypto_secretstream_xchacha20poly1305_tag_final();
    public static final short XCHACHA20POLY1305_TAG_MESSAGE =
            (short) NaCl.sodium().crypto_secretstream_xchacha20poly1305_tag_message();

    static final int XCHACHA20POLY1305_HEADERBYTES = NaCl.sodium().crypto_secretstream_xchacha20poly1305_headerbytes();

    static final int BASE64_ENCODE_FLAGS = Base64.DEFAULT;

    static final String KEYED_WITH = "v1";

    static final byte[] STATE = new byte[XCHACHA20POLY1305_STATEBYTES];

    /*
        Returns a JSON String containing following data:
        {
            "keyedWith": "v1",
            "encryptedKey": "$key_as_base_64",  // The encrypted AES key
            "header": "base_64_encoded_header", // The xchacha20poly1305 stream header
            "messages": []                      // the stream elements, base-64 encoded
        }
    */
    public static String generateJSONEncryptedData(final String publicKeyBase64,
                                                   final String stringData) throws JSONException {
        JSONObject encryptionDataJson = new JSONObject();
        encryptionDataJson.put("keyedWith", KEYED_WITH);

        final byte[] secretKey = createEncryptionKey();
        final byte[] encryptedSecretKey = encryptEncryptionKey(decodeFromBase64(publicKeyBase64), secretKey);
        encryptionDataJson.put("encryptedKey", encodeToBase64(encryptedSecretKey));

        final byte[] encryptedHeader = createEncryptedHeader(secretKey);
        encryptionDataJson.put("header", encodeToBase64(encryptedHeader));

        JSONArray encryptedAndEncodedMessagesJson = new JSONArray();
        if (!stringData.isEmpty()) {
            final String[] messages = stringData.split("\n");
            for (String message : messages) {
                final byte[] encryptedMessage = encryptMessage(message + "\n", XCHACHA20POLY1305_TAG_MESSAGE);
                encryptedAndEncodedMessagesJson.put(encodeToBase64(encryptedMessage));
            }
        }

        final byte[] encryptedDataBase64 = encryptMessage("", XCHACHA20POLY1305_TAG_FINAL);
        encryptedAndEncodedMessagesJson.put(encodeToBase64(encryptedDataBase64));
        encryptionDataJson.put("messages", encryptedAndEncodedMessagesJson);

        return encryptionDataJson.toString();
    }

    private static byte[] decodeFromBase64(String encodedData) {
        return Base64.decode(encodedData, Base64.DEFAULT);
    }

    private static byte[] createEncryptionKey() {
        final byte[] secretKey = new byte[XCHACHA20POLY1305_KEYBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_keygen(secretKey);
        return secretKey;
    }

    private static byte[] encryptEncryptionKey(final byte[] publicKeyBytes,
                                               final byte[] data) {
        final byte[] encryptedData = new byte[XCHACHA20POLY1305_KEYBYTES + BOX_SEALBYTES];
        NaCl.sodium().crypto_box_seal(encryptedData, data, XCHACHA20POLY1305_KEYBYTES, publicKeyBytes);
        return encryptedData;
    }

    private static String encodeToBase64(byte[] data) {
        return Base64.encodeToString(data, BASE64_ENCODE_FLAGS);
    }

    private static byte[] createEncryptedHeader(final byte[] key) {
        final byte[] header = new byte[XCHACHA20POLY1305_HEADERBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_init_push(STATE, header, key);
        return header;
    }

    private static byte[] encryptMessage(final String message,
                                         final short tag) {
        final int[] encryptedDataLengthOutput = new int[0]; // opting not to get this value
        final byte[] additionalData = new byte[0]; // opting not to use this value
        final int additionalDataLength = 0;

        final byte[] dataBytes = message.getBytes();
        final byte[] encryptedMessage = new byte[dataBytes.length + XCHACHA20POLY1305_ABYTES];

        NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
                STATE,
                encryptedMessage,
                encryptedDataLengthOutput,
                dataBytes,
                dataBytes.length,
                additionalData,
                additionalDataLength,
                tag);

        return encryptedMessage;
    }
}

