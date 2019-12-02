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

    /*
        Returns a JSON String containing following data:
        {
            "keyedWith": "v1",
            "encryptedKey": "$key_as_base_64",  // The encrypted AES key
            "header": "base_64_encoded_header", // The xchacha20poly1305 stream header
            "messages": []                      // the stream elements, base-64 encoded
        }
    */
    public static String encryptStringData(final String publicKeyBase64,
                                           final String stringData) throws JSONException {
        JSONObject encryptionDataJson = new JSONObject();
        encryptionDataJson.put("keyedWith", KEYED_WITH);

        // Create data-specific key
        final byte[] key = new byte[XCHACHA20POLY1305_KEYBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_keygen(key);

        final String encryptedKeyBase64 = getBoxSealEncryptedBase64String(
                publicKeyBase64,
                key,
                XCHACHA20POLY1305_KEYBYTES);
        encryptionDataJson.put("encryptedKey", encryptedKeyBase64);

        final byte[] state = new byte[XCHACHA20POLY1305_STATEBYTES];

        final String headerBase64 = initSecretStreamXchacha20poly1305(state, key);
        encryptionDataJson.put("header", headerBase64);

        JSONArray encryptedElementsJson = new JSONArray();
        if (!stringData.isEmpty()) {
            final String[] splitStringData = stringData.split("\n"); // break up the data by line
            
            for (int i = 0; i < splitStringData.length; ++i) {
                String element = splitStringData[i];
                element = element + "\n";
                final String encryptedElementBase64 = getSecretStreamXchacha20poly1305EncryptedBase64String(
                        state,
                        element,
                        XCHACHA20POLY1305_TAG_MESSAGE);
                encryptedElementsJson.put(encryptedElementBase64);
            }
        }

        final String encryptedDataBase64 = getSecretStreamXchacha20poly1305EncryptedBase64String(
                state,
                "",
                XCHACHA20POLY1305_TAG_FINAL);
        encryptedElementsJson.put(encryptedDataBase64);

        encryptionDataJson.put("messages", encryptedElementsJson);

        return encryptionDataJson.toString();
    }

    private static String getBoxSealEncryptedBase64String(final String publicKeyBase64,
                                                          final byte[] data,
                                                          final int dataSize) {
        final byte[] encryptedData = new byte[dataSize + BOX_SEALBYTES];
        final byte[] publicKeyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT);
        NaCl.sodium().crypto_box_seal(encryptedData, data, dataSize, publicKeyBytes);
        return Base64.encodeToString(encryptedData, BASE64_ENCODE_FLAGS);
    }

    private static String initSecretStreamXchacha20poly1305(byte[] state, final byte[] key) {
        final byte[] header = new byte[XCHACHA20POLY1305_HEADERBYTES];
        NaCl.sodium().crypto_secretstream_xchacha20poly1305_init_push(state, header, key);

        return Base64.encodeToString(header, BASE64_ENCODE_FLAGS);
    }

    private static String getSecretStreamXchacha20poly1305EncryptedBase64String(final byte[] state,
                                                                                final String data,
                                                                                final short tag) {
        final int[] encryptedDataLengthOutput = new int[0]; // opting not to get this value
        final byte[] additionalData = new byte[0]; // opting not to use this value
        final int additionalDataLength = 0;

        final byte[] dataBytes = data.getBytes();
        final byte[] encryptedData = new byte[dataBytes.length + XCHACHA20POLY1305_ABYTES];

        NaCl.sodium().crypto_secretstream_xchacha20poly1305_push(
                state,
                encryptedData,
                encryptedDataLengthOutput,
                dataBytes,
                dataBytes.length,
                additionalData,
                additionalDataLength,
                tag);

        return Base64.encodeToString(encryptedData, BASE64_ENCODE_FLAGS);
    }
}

