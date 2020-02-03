package org.wordpress.android.util;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.Sodium;

import java.util.List;

public class EncryptionUtils {
    static final int ABYTES = Sodium.crypto_secretstream_xchacha20poly1305_abytes();
    static final int BOX_SEALBYTES = Sodium.crypto_box_sealbytes();
    static final int KEYBYTES = Sodium.crypto_secretstream_xchacha20poly1305_keybytes();
    static final int STATEBYTES = Sodium.crypto_secretstream_xchacha20poly1305_statebytes();
    static final short TAG_FINAL = (short) Sodium.crypto_secretstream_xchacha20poly1305_tag_final();
    static final short TAG_MESSAGE = (short) Sodium.crypto_secretstream_xchacha20poly1305_tag_message();
    private static final int HEADERBYTES = Sodium.crypto_secretstream_xchacha20poly1305_headerbytes();
    private static final int BASE64_FLAGS = Base64.DEFAULT;
    private static final String KEYED_WITH = "v1";
    private static final byte[] STATE = new byte[STATEBYTES];

    /**
     * This method is in charge of encrypting logs using the symmetric cypher
     * XCHACHA20 and the cryptographic message authentication code (MAC) POLY1305.
     * For more information about the encryption process you can find it here
     * https://libsodium.gitbook.io/doc/secret-key_cryptography/secretstream.
     * @param publicKeyBase64 The public key encoded using Base64.
     * @param logMessages     The list of messages we want to encrypt.
     * @return a JSON String containing following structure:
     *
     * ```
     * {
     * "keyedWith": "v1",
     * "encryptedKey": "<base_64_encrypted_key>",  // The encrypted AES key, base-64 encoded
     * "header": "<base_64_encoded_header>",       // The xchacha20poly1305 stream header
     * "messages": [<base_64_encrypted_msgs>]      // the encrypted log messages, base-64 encoded
     * }
     * ```
     */
    public static String generateJSONEncryptedLogs(final String publicKeyBase64,
                                                   final List<String> logMessages) throws JSONException {

        // Schema version
        JSONObject encryptionDataJson = new JSONObject();
        encryptionDataJson.put("keyedWith", KEYED_WITH);

        // Encryption key
        final byte[] secretKey = createEncryptionKey();
        final byte[] encryptedSecretKey = encryptEncryptionKey(decodeFromBase64(publicKeyBase64), secretKey);
        encryptionDataJson.put("encryptedKey", encodeToBase64(encryptedSecretKey));

        // Header
        final byte[] encryptedHeader = createEncryptedHeader(secretKey);
        encryptionDataJson.put("header", encodeToBase64(encryptedHeader));

        // Log messages
        JSONArray encryptedAndEncodedMessagesJson = new JSONArray();
        for (String message : logMessages) {
            final byte[] encryptedMessage = encryptMessage(message, TAG_MESSAGE);
            encryptedAndEncodedMessagesJson.put(encodeToBase64(encryptedMessage));
        }

        // Final tag
        final byte[] encryptedDataBase64 = encryptMessage("", TAG_FINAL);
        encryptedAndEncodedMessagesJson.put(encodeToBase64(encryptedDataBase64));
        encryptionDataJson.put("messages", encryptedAndEncodedMessagesJson);

        return encryptionDataJson.toString();
    }

    private static byte[] createEncryptionKey() {
        final byte[] secretKey = new byte[KEYBYTES];
        Sodium.crypto_secretstream_xchacha20poly1305_keygen(secretKey);
        return secretKey;
    }

    private static byte[] encryptEncryptionKey(final byte[] publicKeyBytes,
                                               final byte[] data) {
        final byte[] encryptedData = new byte[KEYBYTES + BOX_SEALBYTES];
        Sodium.crypto_box_seal(encryptedData, data, KEYBYTES, publicKeyBytes);
        return encryptedData;
    }

    private static byte[] createEncryptedHeader(final byte[] key) {
        final byte[] header = new byte[HEADERBYTES];
        Sodium.crypto_secretstream_xchacha20poly1305_init_push(STATE, header, key);
        return header;
    }

    private static byte[] encryptMessage(final String message,
                                         final short tag) {
        final int[] encryptedDataLengthOutput = new int[0]; // opting not to get this value
        final byte[] additionalData = new byte[0]; // opting not to use this value
        final int additionalDataLength = 0;
        final byte[] dataBytes = message.getBytes();
        final byte[] encryptedMessage = new byte[dataBytes.length + ABYTES];

        Sodium.crypto_secretstream_xchacha20poly1305_push(
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

    private static String encodeToBase64(byte[] data) {
        return Base64.encodeToString(data, BASE64_FLAGS);
    }

    private static byte[] decodeFromBase64(String encodedData) {
        return Base64.decode(encodedData, Base64.DEFAULT);
    }
}

