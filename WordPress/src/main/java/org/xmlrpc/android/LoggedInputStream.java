package org.xmlrpc.android;

import java.io.IOException;
import java.io.InputStream;


/**
 * A LoggedInputStream adds logging functionality to another input stream.
 * <p>Note that calls on a LoggedInputStream are passed "as-is" to the underlying stream.</p>
 *
 *
 * <p>We're using a LoggedInputStream in {@code XMLRPClient.java} to log the XML-RPC response document in case of parser errors.<br />
 *
 * There are plenty of other ways to log the response, but a {@code XmlPullParser} wants an InputStream as input parameter, and
 * a LoggedInputStream seems the most reliable solution, with the smallest memory footprint.<br />
 * Below are other examples of logging we tried:</p>
 * <ul>
 * <li>Read the first 1000 characters from the original input stream, then create a new SequenceInputStream with both the characters just read (a new ByteArrayInputStream),
 * and the original input stream.</li>
 * <li>Read the whole content in a String and log it, then create an StringInputStream over the string, and pass the new stream to the parser.</li>
 * </ul>
 */

public final class LoggedInputStream extends InputStream {
    private final InputStream inputStream;

    private final static int MAX_LOG_SIZE = 1000;
    private final byte[] loggedString = new byte[MAX_LOG_SIZE];
    private int loggedStringSize = 0;

    public LoggedInputStream(InputStream input) {
        this.inputStream = input;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int bytesRead = inputStream.read(buffer, byteOffset, byteCount);
        if (bytesRead != -1) {
            log(buffer, byteOffset, bytesRead);
        }
        return bytesRead;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    @Override
    public int read() throws IOException {
        int characterRead = inputStream.read();
        if (characterRead != -1) {
            log(characterRead);
        }
        return characterRead;
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return inputStream.skip(byteCount);
    }

    private void log(byte[] inputArray, int byteOffset, int byteCount) {
        int availableSpace = MAX_LOG_SIZE - loggedStringSize;
        if (availableSpace <= 0) {
            return;
        }
        int bytesLength = Math.min(availableSpace, byteCount);
        int startingPosition = MAX_LOG_SIZE - availableSpace;
        System.arraycopy(inputArray, byteOffset, loggedString, startingPosition, bytesLength);
        loggedStringSize += bytesLength;
    }

    private void log(int inputChar) {
        byte[] logThis = {(byte) inputChar};
        log(logThis, 0, 1);
    }

    public String getResponseDocument() {
        if (loggedStringSize == 0) {
            return "";
        } else {
            return new String(loggedString, 0, loggedStringSize);
        }
    }
}
