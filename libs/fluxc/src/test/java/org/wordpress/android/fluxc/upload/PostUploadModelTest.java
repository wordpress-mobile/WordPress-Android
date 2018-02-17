package org.wordpress.android.fluxc.upload;

import android.text.TextUtils;

import org.junit.Test;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PostUploadModelTest {
    @Test
    public void testEquals() {
        PostUploadModel postUploadModel1 = new PostUploadModel(1);
        PostUploadModel postUploadModel2 = new PostUploadModel(1);

        Set<Integer> idSet = new HashSet<>();
        idSet.add(6);
        idSet.add(5);
        postUploadModel1.setAssociatedMediaIdSet(idSet);
        assertFalse(postUploadModel1.equals(postUploadModel2));

        postUploadModel2.setAssociatedMediaIdSet(idSet);

        PostError postError = new PostError(PostErrorType.UNKNOWN_POST, "Unknown post");
        postUploadModel1.setPostError(postError);

        assertFalse(postUploadModel1.equals(postUploadModel2));

        postUploadModel2.setErrorType(postError.type.toString());
        postUploadModel2.setErrorMessage(postError.message);

        assertTrue(postUploadModel1.equals(postUploadModel2));
    }

    @Test
    public void testAssociatedMediaIds() {
        PostUploadModel postUploadModel = new PostUploadModel(1);
        Set<Integer> idSet = new HashSet<>();
        idSet.add(6);
        idSet.add(5);
        postUploadModel.setAssociatedMediaIdSet(idSet);
        assertEquals("5,6", postUploadModel.getAssociatedMediaIds());
        assertTrue(idSet.containsAll(postUploadModel.getAssociatedMediaIdSet()));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().containsAll(idSet));
    }

    @Test
    public void testPostError() {
        PostUploadModel postUploadModel = new PostUploadModel(1);

        assertNull(postUploadModel.getPostError());
        assertTrue(TextUtils.isEmpty(postUploadModel.getErrorType()));
        assertTrue(TextUtils.isEmpty(postUploadModel.getErrorMessage()));

        postUploadModel.setPostError(new PostError(PostErrorType.UNKNOWN_POST, "Unknown post"));
        assertNotNull(postUploadModel.getPostError());
        assertEquals(PostErrorType.UNKNOWN_POST, PostErrorType.fromString(postUploadModel.getErrorType()));
        assertEquals("Unknown post", postUploadModel.getErrorMessage());
    }
}
