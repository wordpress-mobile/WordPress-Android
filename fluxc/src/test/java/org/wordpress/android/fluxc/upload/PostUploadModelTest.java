package org.wordpress.android.fluxc.upload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PostUploadModelTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testEquals() {
        PostUploadModel postUploadModel1 = new PostUploadModel(1);
        PostUploadModel postUploadModel2 = new PostUploadModel(1);

        Set<Integer> idSet = new HashSet<>();
        idSet.add(6);
        idSet.add(5);
        postUploadModel1.setAssociatedMediaIdSet(idSet);
        postUploadModel2.setAssociatedMediaIdSet(idSet);

        PostError postError = new PostError(PostErrorType.UNKNOWN_POST, "Unknown post");
        postUploadModel1.setPostError(postError);
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

    private PostUploadModel getPostUploadModel() {
        PostUploadModel testModel = new PostUploadModel();
        testModel.setId(1);
        return testModel;
    }
}
