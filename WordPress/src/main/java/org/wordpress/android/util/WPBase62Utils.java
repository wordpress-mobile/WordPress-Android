package org.wordpress.android.util;

public class WPBase62Utils {
    // symbols array
    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Converts base-N to base-10, for N <= 62
     * Inputs prefixed with underscores will be treated as base-( 62 - number_of_underscores )
     *
     * @param number $num base-N number
     *
     * @return long base-10 number
     */
    public static long sixtwo2dec(String number)
    {
        int base = 62;
        while (number.startsWith("_")) {
            base -= 1;
            number = number.substring(1);

            if (base < 50) {
                // this is getting too low, just bail
                return 0;
            }
        }

        long result = 0;
        int position = number.length(); //we start from the last digit in a String (lowest value)
        for (char ch : number.toCharArray())
        {
            int value = SYMBOLS.indexOf(ch);
            result += value * pow(base,--position);

        }
        return result;
    }

    private static long pow(int value, int x)
    {
        if (x == 0) {
            return 1;
        }
        return value * pow(value, x-1);
    }
}
