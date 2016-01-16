package org.wordpress.android.util;

import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.test.mock.MockCursor;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.DatabaseUtils.*;

public class DatabaseUtilsTest extends InstrumentationTestCase {
    public void testInvalidIntFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(INVALID_INTEGER, getIntFromCursor(testCursor, INVALID_COLUMN_NAME));
    }

    public void testValidIntFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(VALID_INTEGER, getIntFromCursor(testCursor, INTEGER_COLUMN_NAME));
    }

    public void testInvalidLongFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(INVALID_INTEGER, getIntFromCursor(testCursor, INVALID_COLUMN_NAME));
    }

    public void testValidLongFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(VALID_INTEGER, getIntFromCursor(testCursor, INTEGER_COLUMN_NAME));
    }

    public void testInvalidStringFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(INVALID_STRING, getStringFromCursor(testCursor, INVALID_COLUMN_NAME));
    }

    public void testValidStringFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(VALID_STRING, getStringFromCursor(testCursor, STRING_COLUMN_NAME));
    }

    public void testInvalidBoolFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(false, getBooleanFromCursor(testCursor, INVALID_COLUMN_NAME));
    }

    public void testValidBoolFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(true, getBooleanFromCursor(testCursor, BOOL_COLUMN_NAME));
    }

    public void testStringListWithNullList() {
        assertEquals("", separatedStringList(null, ""));
    }

    public void testStringListWithNullSeparator() {
        assertEquals("4null3null2null1null0", separatedStringList(getSimpleTestList(5), null));
    }

    public void testStringListWithEmptyList() {
        assertEquals("", separatedStringList(getSimpleTestList(0), ","));
    }

    public void testStringListWithEmptySeparator() {
        assertEquals("43210", separatedStringList(getSimpleTestList(5), ""));
    }

    public void testStringListWithLongSeparator() {
        assertEquals("4://,3://,2://,1://,0", separatedStringList(getSimpleTestList(5), "://,"));
    }

    public void testStringListWithNonString() {
        assertEquals("default,this be a long string with spaces,three,two,one,zero", separatedStringList(getNonStringTestList(6), ","));
    }

    private class TestCursor extends MockCursor {
        @Override
        public int getColumnIndex(String columnName) {
            switch (columnName) {
                case INTEGER_COLUMN_NAME:
                    return INTEGER_COLUMN_INDEX;
                case LONG_COLUMN_NAME:
                    return LONG_COLUMN_INDEX;
                case STRING_COLUMN_NAME:
                    return STRING_COLUMN_INDEX;
                case BOOL_COLUMN_NAME:
                    return BOOL_COLUMN_INDEX;
                default:
                    return INVALID_COLUMN_INDEX;
            }
        }

        @Override
        public long getLong(int columnIndex) {
            switch (columnIndex) {
                case LONG_COLUMN_INDEX:
                    return VALID_LONG;
                default:
                    return INVALID_INTEGER;
            }
        }

        @Override
        public int getInt(int columnIndex) {
            switch (columnIndex) {
                case INTEGER_COLUMN_INDEX:
                    return VALID_INTEGER;
                case BOOL_COLUMN_INDEX:
                    return VALID_BOOL;
                default:
                    return INVALID_INTEGER;
            }
        }

        @Override
        public String getString(int columnIndex) {
            switch (columnIndex) {
                case STRING_COLUMN_INDEX:
                    return VALID_STRING;
                default:
                    return INVALID_STRING;
            }
        }
    }

    private class NonString {
        private int mVal;

        private NonString() {}

        public NonString(int val) { this(); mVal = val; }

        @Override
        public String toString() {
            switch (mVal) {
                case 0:
                    return "zero";
                case 1:
                    return "one";
                case 2:
                    return "two";
                case 3:
                    return "three";
                case 4:
                    return "this be a long string with spaces";
                default:
                    return "default";
            }
        }
    }

    private List<String> getSimpleTestList(int size) {
        List<String> testList = new ArrayList<>();
        while (size-- > 0) {
            testList.add(String.valueOf(size));
        }
        return testList;
    }

    private List<NonString> getNonStringTestList(int size) {
        List<NonString> testList = new ArrayList<>();
        while (size-- > 0) {
            testList.add(new NonString(size));
        }
        return testList;
    }

    //
    // Test values
    //

    private static final int INVALID_INTEGER = -1;
    private static final int VALID_INTEGER = 1;
    private static final long VALID_LONG = Long.MAX_VALUE;
    private static final String INVALID_STRING = "";
    private static final String VALID_STRING = "DatabaseUtilsTest_validString";
    private static final int VALID_BOOL = 1;
    private static final String INVALID_COLUMN_NAME = "DatabaseUtilsTest_invalidColumnName";
    private static final String INTEGER_COLUMN_NAME = "DatabaseUtilsTest_integerColumnName";
    private static final String LONG_COLUMN_NAME = "DatabaseUtilsTest_longColumnName";
    private static final String STRING_COLUMN_NAME = "DatabaseUtilsTest_stringColumnName";
    private static final String BOOL_COLUMN_NAME = "DatabaseUtilsTest_boolColumnName";
    private static final int INVALID_COLUMN_INDEX = -1;
    private static final int INTEGER_COLUMN_INDEX = 0;
    private static final int LONG_COLUMN_INDEX = 1;
    private static final int STRING_COLUMN_INDEX = 2;
    private static final int BOOL_COLUMN_INDEX = 3;
}
