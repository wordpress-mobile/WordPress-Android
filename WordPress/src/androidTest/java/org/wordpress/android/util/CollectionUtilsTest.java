package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtilsTest extends InstrumentationTestCase {
    public void testNullListsEquality() {
        assertTrue(CollectionUtils.areListsEqual(null, null));
    }

    public void testOneNullListEquality() {
        List<Object> testList = new ArrayList<>();
        assertFalse(CollectionUtils.areListsEqual(null, testList));
        assertFalse(CollectionUtils.areListsEqual(testList, null));
    }

    public void testEmptyListsEquality() {
        List<Object> testList1 = new ArrayList<>();
        List<Object> testList2 = new ArrayList<>();
        assertTrue(CollectionUtils.areListsEqual(testList1, testList2));
        assertTrue(CollectionUtils.areListsEqual(testList2, testList1));
    }

    public void testSameListEquality() {
        List<Object> testList1 = new ArrayList<>();
        testList1.add("1"); testList1.add("2"); testList1.add("3");
        List<Object> testList2 = new ArrayList<>();
        testList2.add("1"); testList2.add("2"); testList2.add("3");
        assertTrue(CollectionUtils.areListsEqual(testList1, testList1));
        assertTrue(CollectionUtils.areListsEqual(testList1, testList2));
        assertTrue(CollectionUtils.areListsEqual(testList2, testList1));
    }

    public void testDifferentSizeListsEquality() {
        List<Object> testList1 = new ArrayList<>();
        testList1.add("1"); testList1.add("2"); testList1.add("3");
        List<Object> testList2 = new ArrayList<>();
        testList2.add("1"); testList2.add("2"); testList2.add("3"); testList2.add("4");
        assertFalse(CollectionUtils.areListsEqual(testList1, testList2));
        assertFalse(CollectionUtils.areListsEqual(testList2, testList1));
    }

    public void testDifferentOrderListEquality() {
        List<Object> testList1 = new ArrayList<>();
        testList1.add("1"); testList1.add("2"); testList1.add("3");
        List<Object> testList2 = new ArrayList<>();
        testList2.add("3"); testList2.add("2"); testList2.add("1");
        assertFalse(CollectionUtils.areListsEqual(testList1, testList2));
        assertFalse(CollectionUtils.areListsEqual(testList2, testList1));
    }
}
