package org.wordpress.android.util;

import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.test.mock.MockCursor;

import static org.wordpress.android.util.DatabaseUtils.*;

public class DatabaseUtilsTest extends InstrumentationTestCase {
    private static final int INVALID_INTEGER = -1;
    private static final int VALID_INTEGER = 1;
    private static final String INVALID_STRING = "";
    private static final String VALID_STRING = "DatabaseUtilsTest_validString";
    private static final int VALID_BOOL = 1;
    private static final String INVALID_COLUMN_NAME = "DatabaseUtilsTest_invalidColumnName";
    private static final String INTEGER_COLUMN_NAME = "DatabaseUtilsTest_integerColumnName";
    private static final String STRING_COLUMN_NAME = "DatabaseUtilsTest_stringColumnName";
    private static final String BOOL_COLUMN_NAME = "DatabaseUtilsTest_boolColumnName";
    private static final int INVALID_COLUMN_INDEX = -1;
    private static final int INTEGER_COLUMN_INDEX = 0;
    private static final int STRING_COLUMN_INDEX = 1;
    private static final int BOOL_COLUMN_INDEX = 2;

    public void testInvalidIntFromCursor() {
        Cursor testCursor = new TestCursor();
        assertEquals(INVALID_INTEGER, getIntFromCursor(testCursor, INVALID_COLUMN_NAME));
    }

    public void testValidIntFromCursor() {
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

    private class TestCursor extends MockCursor {
        @Override
        public int getColumnIndex(String columnName) {
            switch (columnName) {
                case INTEGER_COLUMN_NAME:
                    return INTEGER_COLUMN_INDEX;
                case STRING_COLUMN_NAME:
                    return STRING_COLUMN_INDEX;
                case BOOL_COLUMN_NAME:
                    return BOOL_COLUMN_INDEX;
                default:
                    return INVALID_COLUMN_INDEX;
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
}
