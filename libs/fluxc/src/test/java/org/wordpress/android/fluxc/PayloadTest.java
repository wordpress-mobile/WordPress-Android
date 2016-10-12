package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.BaseRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PayloadTest {

    private class CloneablePayload extends Payload implements Cloneable {
        @Override
        public CloneablePayload clone() {
            try {
                return (CloneablePayload) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(); // Can't happen
            }
        }
    }

    @Test
    public void testClone() {
        // Cloning default (no error) payload
        CloneablePayload errorlessPayload = new CloneablePayload();

        CloneablePayload errorlessClone = errorlessPayload.clone();

        assertFalse(errorlessPayload == errorlessClone);
        assertNull(errorlessPayload.error);
        assertNull(errorlessClone.error);

        // Cloning payload with error field
        CloneablePayload errorPayload = new CloneablePayload();

        errorPayload.error = new BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.SERVER_ERROR);

        CloneablePayload errorClone = errorPayload.clone();

        assertFalse(errorPayload == errorClone);

        // The error field should be cloned
        assertNotEquals(errorClone.error, errorPayload.error);
        assertEquals(errorClone.error.type, errorPayload.error.type);
    }
}
