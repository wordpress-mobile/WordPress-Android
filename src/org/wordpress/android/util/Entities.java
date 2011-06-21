package org.wordpress.android.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * Provides HTML and XML entity utilities.
 * </p>
 * 
 * @see <a href="http://hotwired.lycos.com/webmonkey/reference/special_characters/">ISO Entities</a>
 * @see <a href="http://www.w3.org/TR/REC-html32#latin1">HTML 3.2 Character Entities for ISO Latin-1</a>
 * @see <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">HTML 4.0 Character entity references</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#h-5.3">HTML 4.01 Character References</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#code-position">HTML 4.01 Code positions</a>
 * 
 * @author <a href="mailto:alex@purpletech.com">Alexander Day Chaffee</a>
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @since 2.0
 * @version $Id: Entities.java 636641 2008-03-13 06:11:30Z bayard $
 */
class Entities {

    private static final String[][] BASIC_ARRAY = {
    	{"quot", "34"}, 
        {"amp", "38"}, 
        {"lt", "60"}, 
        {"gt", "62"} 
    };

    private static final String[][] APOS_ARRAY = {{"apos", "39"}, 
    };

    // package scoped for testing
    static final String[][] ISO8859_1_ARRAY = {{"nbsp", "160"},
        {"iexcl", "161"},
        {"cent", "162"}, 
        {"pound", "163"}, 
        {"curren", "164"}, 
        {"yen", "165"}, 
        {"brvbar", "166"}, 
        {"sect", "167"}, 
        {"uml", "168"}, 
        {"copy", "169"}, 
        {"ordf", "170"}, 
        {"laquo", "171"}, 
        {"not", "172"}, 
        {"shy", "173"}, 
        {"reg", "174"}, 
        {"macr", "175"}, 
        {"deg", "176"}, 
        {"plusmn", "177"},
        {"sup2", "178"},
        {"sup3", "179"}, 
        {"acute", "180"}, 
        {"micro", "181"},
        {"para", "182"}, 
        {"middot", "183"}, 
        {"cedil", "184"}, 
        {"sup1", "185"}, 
        {"ordm", "186"}, 
        {"raquo", "187"}, 
        {"frac14", "188"}, 
        {"frac12", "189"}, 
        {"frac34", "190"}, 
        {"iquest", "191"}, 
        {"Agrave", "192"}, 
        {"Aacute", "193"}, 
        {"Acirc", "194"}, 
        {"Atilde", "195"}, 
        {"Auml", "196"}, 
        {"Aring", "197"}, 
        {"AElig", "198"}, 
        {"Ccedil", "199"}, 
        {"Egrave", "200"}, 
        {"Eacute", "201"}, 
        {"Ecirc", "202"}, 
        {"Euml", "203"}, 
        {"Igrave", "204"}, 
        {"Iacute", "205"}, 
        {"Icirc", "206"}, 
        {"Iuml", "207"}, 
        {"ETH", "208"}, 
        {"Ntilde", "209"}, 
        {"Ograve", "210"}, 
        {"Oacute", "211"}, 
        {"Ocirc", "212"}, 
        {"Otilde", "213"}, 
        {"Ouml", "214"}, 
        {"times", "215"}, 
        {"Oslash", "216"}, 
        {"Ugrave", "217"}, 
        {"Uacute", "218"}, 
        {"Ucirc", "219"}, 
        {"Uuml", "220"}, 
        {"Yacute", "221"}, 
        {"THORN", "222"}, 
        {"szlig", "223"}, 
        {"agrave", "224"}, 
        {"aacute", "225"}, 
        {"acirc", "226"}, 
        {"atilde", "227"}, 
        {"auml", "228"}, 
        {"aring", "229"}, 
        {"aelig", "230"}, 
        {"ccedil", "231"}, 
        {"egrave", "232"}, 
        {"eacute", "233"}, 
        {"ecirc", "234"}, 
        {"euml", "235"}, 
        {"igrave", "236"}, 
        {"iacute", "237"}, 
        {"icirc", "238"}, 
        {"iuml", "239"}, 
        {"eth", "240"}, 
        {"ntilde", "241"}, 
        {"ograve", "242"}, 
        {"oacute", "243"}, 
        {"ocirc", "244"}, 
        {"otilde", "245"}, 
        {"ouml", "246"}, 
        {"divide", "247"}, 
        {"oslash", "248"}, 
        {"ugrave", "249"}, 
        {"uacute", "250"}, 
        {"ucirc", "251"}, 
        {"uuml", "252"}, 
        {"yacute", "253"}, 
        {"thorn", "254"}, 
        {"yuml", "255"}, 
    };

    // http://www.w3.org/TR/REC-html40/sgml/entities.html
    // package scoped for testing
    static final String[][] HTML40_ARRAY = {
    // <!-- Latin Extended-B -->
        {"fnof", "402"}, 
        {"Alpha", "913"}, 
        {"Beta", "914"}, 
        {"Gamma", "915"}, 
        {"Delta", "916"},
        {"Epsilon", "917"}, 
        {"Zeta", "918"}, 
        {"Eta", "919"}, 
        {"Theta", "920"}, 
        {"Iota", "921"}, 
        {"Kappa", "922"}, 
        {"Lambda", "923"},
        {"Mu", "924"},
        {"Nu", "925"}, 
        {"Xi", "926"},
        {"Omicron", "927"}, 
        {"Pi", "928"}, 
        {"Rho", "929"}, 
        // <!-- there is no Sigmaf, and no U+03A2 character either -->
        {"Sigma", "931"}, 
        {"Tau", "932"}, 
        {"Upsilon", "933"}, 
        {"Phi", "934"},
        {"Chi", "935"},
        {"Psi", "936"},
        {"Omega", "937"}, 
        {"alpha", "945"}, 
        {"beta", "946"}, 
        {"gamma", "947"}, 
        {"delta", "948"}, 
        {"epsilon", "949"}, 
        {"zeta", "950"},
        {"eta", "951"}, 
        {"theta", "952"}, 
        {"iota", "953"}, 
        {"kappa", "954"}, 
        {"lambda", "955"}, 
        {"mu", "956"},
        {"nu", "957"}, 
        {"xi", "958"}, 
        {"omicron", "959"}, 
        {"pi", "960"}, 
        {"rho", "961"}, 
        {"sigmaf", "962"}, 
        {"sigma", "963"}, 
        {"tau", "964"}, 
        {"upsilon", "965"}, 
        {"phi", "966"}, 
        {"chi", "967"}, 
        {"psi", "968"},
        {"omega", "969"}, 
        {"thetasym", "977"}, 
        {"upsih", "978"}, 
        {"piv", "982"}, 
        // <!-- General Punctuation -->
        {"bull", "8226"}, 
        // <!-- bullet is NOT the same as bullet operator, U+2219 -->
        {"hellip", "8230"}, 
        {"prime", "8242"}, 
        {"Prime", "8243"}, 
        {"oline", "8254"}, 
        {"frasl", "8260"}, 
        // <!-- Letterlike Symbols -->
        {"weierp", "8472"},
        {"image", "8465"}, 
        {"real", "8476"}, 
        {"trade", "8482"}, 
        {"alefsym", "8501"}, 
        // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
        // same glyph could be used to depict both characters -->
        // <!-- Arrows -->
        {"larr", "8592"},
        {"uarr", "8593"}, 
        {"rarr", "8594"}, 
        {"darr", "8595"}, 
        {"harr", "8596"}, 
        {"crarr", "8629"},
        {"lArr", "8656"}, 
        // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
        // arrow but also does not have any other character for that function.
        // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
        {"uArr", "8657"}, 
        {"rArr", "8658"}, 
        // <!-- ISO 10646 does not say this is the 'implies' character but does not
        // have another character with this function so ?rArr can be used for
        // 'implies' as ISOtech suggests -->
        {"dArr", "8659"}, 
        {"hArr", "8660"}, 
        // <!-- Mathematical Operators -->
        {"forall", "8704"}, 
        {"part", "8706"},
        {"exist", "8707"}, 
        {"empty", "8709"}, 
        {"nabla", "8711"}, 
        {"isin", "8712"},
        {"notin", "8713"}, 
        {"ni", "8715"}, 
        // <!-- should there be a more memorable name than 'ni'? -->
        {"prod", "8719"}, 
        // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
        // though the same glyph might be used for both -->
        {"sum", "8721"},
        // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
        // though the same glyph might be used for both -->
        {"minus", "8722"}, 
        {"lowast", "8727"}, 
        {"radic", "8730"}, 
        {"prop", "8733"}, 
        {"infin", "8734"}, 
        {"ang", "8736"}, 
        {"and", "8743"}, 
        {"or", "8744"}, 
        {"cap", "8745"}, 
        {"cup", "8746"}, 
        {"int", "8747"}, 
        {"there4", "8756"}, 
        {"sim", "8764"}, // tilde operator = varies with = similar to,U+223C ISOtech -->
        // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
        // the same glyph might be used to represent both -->
        {"cong", "8773"}, 
        {"asymp", "8776"},
        {"ne", "8800"}, 
        {"equiv", "8801"}, 
        {"le", "8804"}, 
        {"ge", "8805"}, 
        {"sub", "8834"}, 
        {"sup", "8835"}, 
        // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
        // Symbol font encoding and is not included. Should it be, for symmetry?
        // It is in ISOamsn --> <!ENTITY nsub", "8836"},
        // not a subset of, U+2284 ISOamsn -->
        {"sube", "8838"}, 
        {"supe", "8839"}, 
        {"oplus", "8853"}, 
        {"otimes", "8855"},
        {"perp", "8869"}, 
        {"sdot", "8901"}, 
        // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
        // <!-- Miscellaneous Technical -->
        {"lceil", "8968"}, 
        {"rceil", "8969"}, 
        {"lfloor", "8970"}, 
        {"rfloor", "8971"}, 
        {"lang", "9001"}, 
        // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
        // mark' -->
        {"rang", "9002"}, 
        // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
        // 'single right-pointing angle quotation mark' -->
        // <!-- Geometric Shapes -->
        {"loz", "9674"}, 
        // <!-- Miscellaneous Symbols -->
        {"spades", "9824"}, 
        // <!-- black here seems to mean filled as opposed to hollow -->
        {"clubs", "9827"}, 
        {"hearts", "9829"},
        {"diams", "9830"},

        // <!-- Latin Extended-A -->
        {"OElig", "338"}, 
        {"oelig", "339"}, 
        // <!-- ligature is a misnomer, this is a separate character in some languages -->
        {"Scaron", "352"}, 
        {"scaron", "353"}, 
        {"Yuml", "376"}, 
        // <!-- Spacing Modifier Letters -->
        {"circ", "710"}, 
        {"tilde", "732"}, 
        // <!-- General Punctuation -->
        {"ensp", "8194"},
        {"emsp", "8195"}, 
        {"thinsp", "8201"}, 
        {"zwnj", "8204"}, 
        {"zwj", "8205"},
        {"lrm", "8206"},
        {"rlm", "8207"},
        {"ndash", "8211"},
        {"mdash", "8212"},
        {"lsquo", "8216"},
        {"rsquo", "8217"},
        {"sbquo", "8218"},
        {"ldquo", "8220"},
        {"rdquo", "8221"},
        {"bdquo", "8222"},
        {"dagger", "8224"},
        {"Dagger", "8225"},
        {"permil", "8240"}, 
        {"lsaquo", "8249"}, 
        // <!-- lsaquo is proposed but not yet ISO standardized -->
        {"rsaquo", "8250"}, 
        // <!-- rsaquo is proposed but not yet ISO standardized -->
        {"euro", "8364"}, 
    };

    /**
     * <p>
     * The set of entities supported by standard XML.
     * </p>
     */
    public static final Entities XML;

    /**
     * <p>
     * The set of entities supported by HTML 3.2.
     * </p>
     */
    public static final Entities HTML32;

    /**
     * <p>
     * The set of entities supported by HTML 4.0.
     * </p>
     */
    public static final Entities HTML40;
    public static final Entities HTML40_escape;
    static {
        XML = new Entities();
        XML.addEntities(BASIC_ARRAY);
        XML.addEntities(APOS_ARRAY);
    }

    static {
        HTML32 = new Entities();
        HTML32.addEntities(BASIC_ARRAY);
        HTML32.addEntities(ISO8859_1_ARRAY);
    }

    static {
        HTML40 = new Entities();
        fillWithHtml40Entities(HTML40);
        HTML40_escape = new Entities();
        fillWithHtml40EntitiesEscape(HTML40);
    }

    /**
     * <p>
     * Fills the specified entities instance with HTML 40 entities.
     * </p>
     * 
     * @param entities
     *            the instance to be filled.
     */
    static void fillWithHtml40Entities(Entities entities) {
        entities.addEntities(BASIC_ARRAY);
        entities.addEntities(ISO8859_1_ARRAY);
        entities.addEntities(HTML40_ARRAY);
    }
    
    static void fillWithHtml40EntitiesEscape(Entities entities) {
        //entities.addEntities(BASIC_ARRAY);
        entities.addEntities(ISO8859_1_ARRAY);
        entities.addEntities(HTML40_ARRAY);
    }

    static interface EntityMap {
        /**
         * <p>
         * Add an entry to this entity map.
         * </p>
         * 
         * @param name
         *            the entity name
         * @param value
         *            the entity value
         */
        void add(String name, int value);

        /**
         * <p>
         * Returns the name of the entity identified by the specified value.
         * </p>
         * 
         * @param value
         *            the value to locate
         * @return entity name associated with the specified value
         */
        String name(int value);

        /**
         * <p>
         * Returns the value of the entity identified by the specified name.
         * </p>
         * 
         * @param name
         *            the name to locate
         * @return entity value associated with the specified name
         */
        int value(String name);
    }

    static class PrimitiveEntityMap implements EntityMap {
        private Map<String, Integer> mapNameToValue = new HashMap<String, Integer>();

        private IntHashMap mapValueToName = new IntHashMap();

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, new Integer(value));
            mapValueToName.put(value, name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return (String) mapValueToName.get(value);
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    static abstract class MapIntMap implements Entities.EntityMap {
        protected Map<String, Integer> mapNameToValue;

        protected Map<Integer, String> mapValueToName;

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, new Integer(value));
            mapValueToName.put(new Integer(value), name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return (String) mapValueToName.get(new Integer(value));
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    static class HashEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>HashEntityMap</code>.
         */
        public HashEntityMap() {
            mapNameToValue = new HashMap<String, Integer>();
            mapValueToName = new HashMap<Integer, String>();
        }
    }

    static class TreeEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>TreeEntityMap</code>.
         */
        public TreeEntityMap() {
            mapNameToValue = new TreeMap<String, Integer>();
            mapValueToName = new TreeMap<Integer, String>();
        }
    }

    static class LookupEntityMap extends PrimitiveEntityMap {
        private String[] lookupTable;

        private int LOOKUP_TABLE_SIZE = 256;

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            if (value < LOOKUP_TABLE_SIZE) {
                return lookupTable()[value];
            }
            return super.name(value);
        }

        /**
         * <p>
         * Returns the lookup table for this entity map. The lookup table is created if it has not been previously.
         * </p>
         * 
         * @return the lookup table
         */
        private String[] lookupTable() {
            if (lookupTable == null) {
                createLookupTable();
            }
            return lookupTable;
        }

        /**
         * <p>
         * Creates an entity lookup table of LOOKUP_TABLE_SIZE elements, initialized with entity names.
         * </p>
         */
        private void createLookupTable() {
            lookupTable = new String[LOOKUP_TABLE_SIZE];
            for (int i = 0; i < LOOKUP_TABLE_SIZE; ++i) {
                lookupTable[i] = super.name(i);
            }
        }
    }

    static class ArrayEntityMap implements EntityMap {
        protected int growBy = 100;

        protected int size = 0;

        protected String[] names;

        protected int[] values;

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code>.
         */
        public ArrayEntityMap() {
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying the size by which the array should
         * grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public ArrayEntityMap(int growBy) {
            this.growBy = growBy;
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            names[size] = name;
            values[size] = value;
            size++;
        }

        /**
         * Verifies the capacity of the entity array, adjusting the size if necessary.
         * 
         * @param capacity
         *            size the array should be
         */
        protected void ensureCapacity(int capacity) {
            if (capacity > names.length) {
                int newSize = Math.max(capacity, size + growBy);
                String[] newNames = new String[newSize];
                System.arraycopy(names, 0, newNames, 0, size);
                names = newNames;
                int[] newValues = new int[newSize];
                System.arraycopy(values, 0, newValues, 0, size);
                values = newValues;
            }
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            for (int i = 0; i < size; ++i) {
                if (values[i] == value) {
                    return names[i];
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            for (int i = 0; i < size; ++i) {
                if (names[i].equals(name)) {
                    return values[i];
                }
            }
            return -1;
        }
    }

    static class BinaryEntityMap extends ArrayEntityMap {

        /**
         * Constructs a new instance of <code>BinaryEntityMap</code>.
         */
        public BinaryEntityMap() {
            super();
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying the size by which the underlying array
         * should grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public BinaryEntityMap(int growBy) {
            super(growBy);
        }

        /**
         * Performs a binary search of the entity array for the specified key. This method is based on code in
         * {@link java.util.Arrays}.
         * 
         * @param key
         *            the key to be found
         * @return the index of the entity array matching the specified key
         */
        private int binarySearch(int key) {
            int low = 0;
            int high = size - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = values[mid];

                if (midVal < key) {
                    low = mid + 1;
                } else if (midVal > key) {
                    high = mid - 1;
                } else {
                    return mid; // key found
                }
            }
            return -(low + 1); // key not found.
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            int insertAt = binarySearch(value);
            if (insertAt > 0) {
                return; // note: this means you can't insert the same value twice
            }
            insertAt = -(insertAt + 1); // binarySearch returns it negative and off-by-one
            System.arraycopy(values, insertAt, values, insertAt + 1, size - insertAt);
            values[insertAt] = value;
            System.arraycopy(names, insertAt, names, insertAt + 1, size - insertAt);
            names[insertAt] = name;
            size++;
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            int index = binarySearch(value);
            if (index < 0) {
                return null;
            }
            return names[index];
        }
    }

    // package scoped for testing
    EntityMap map = new Entities.LookupEntityMap();

    /**
     * <p>
     * Adds entities to this entity.
     * </p>
     * 
     * @param entityArray
     *            array of entities to be added
     */
    public void addEntities(String[][] entityArray) {
        for (int i = 0; i < entityArray.length; ++i) {
            addEntity(entityArray[i][0], Integer.parseInt(entityArray[i][1]));
        }
    }

    /**
     * <p>
     * Add an entity to this entity.
     * </p>
     * 
     * @param name
     *            name of the entity
     * @param value
     *            vale of the entity
     */
    public void addEntity(String name, int value) {
        map.add(name, value);
    }

    /**
     * <p>
     * Returns the name of the entity identified by the specified value.
     * </p>
     * 
     * @param value
     *            the value to locate
     * @return entity name associated with the specified value
     */
    public String entityName(int value) {
        return map.name(value);
    }

    /**
     * <p>
     * Returns the value of the entity identified by the specified name.
     * </p>
     * 
     * @param name
     *            the name to locate
     * @return entity value associated with the specified name
     */
    public int entityValue(String name) {
        return map.value(name);
    }

    /**
     * <p>
     * Escapes the characters in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1), escape(&quot;\u00A1&quot;) will return
     * &quot;&amp;foo;&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String escape(String str) {
        StringWriter stringWriter = createStringWriter(str);
        try {
            this.escape(stringWriter, str);
        } catch (IOException e) {
            // This should never happen because ALL the StringWriter methods called by #escape(Writer, String) do not
            // throw IOExceptions.
            //throw new UnhandledException(e);
        }
        return stringWriter.toString();
    }

    /**
     * <p>
     * Escapes the characters in the <code>String</code> passed and writes the result to the <code>Writer</code>
     * passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results of the escaping to. Assumed to be a non-null value.
     * @param str
     *            The <code>String</code> to escape. Assumed to be a non-null value.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void escape(Writer writer, String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            String entityName = this.entityName(c);
            if (entityName == null) {
                if (c > 0x7F) {
                    writer.write("&#");
                    writer.write(Integer.toString(c, 10));
                    writer.write(';');
                } else {
                    writer.write(c);
                }
            } else {
                writer.write('&');
                writer.write(entityName);
                writer.write(';');
            }
        }
    }

    /**
     * <p>
     * Unescapes the entities in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1), unescape(&quot;&amp;foo;&quot;) will return
     * &quot;\u00A1&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String unescape(String str) {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            return str;
        } else {
            StringWriter stringWriter = createStringWriter(str);
            try {
                this.doUnescape(stringWriter, str, firstAmp);
            } catch (IOException e) {
                // This should never happen because ALL the StringWriter methods called by #escape(Writer, String) 
                // do not throw IOExceptions.
               // throw new UnhandledException(e);
            }
            return stringWriter.toString();
        }
    }

    /**
     * Make the StringWriter 10% larger than the source String to avoid growing the writer
     *
     * @param str The source string
     * @return A newly created StringWriter
     */
    private StringWriter createStringWriter(String str) {
        return new StringWriter((int) (str.length() + (str.length() * 0.1)));
    }

    /**
     * <p>
     * Unescapes the escaped entities in the <code>String</code> passed and writes the result to the
     * <code>Writer</code> passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be non-null.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void unescape(Writer writer, String str) throws IOException {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            writer.write(str);
            return;
        } else {
            doUnescape(writer, str, firstAmp);
        }
    }

    /**
     * Underlying unescape method that allows the optimisation of not starting from the 0 index again.
     *
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be non-null.
     * @param firstAmp
     *            The <code>int</code> index of the first ampersand in the source String.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     */
    private void doUnescape(Writer writer, String str, int firstAmp) throws IOException {
        writer.write(str, 0, firstAmp);
        int len = str.length();
        for (int i = firstAmp; i < len; i++) {
            char c = str.charAt(i);
            if (c == '&') {
                int nextIdx = i + 1;
                int semiColonIdx = str.indexOf(';', nextIdx);
                if (semiColonIdx == -1) {
                    writer.write(c);
                    continue;
                }
                int amphersandIdx = str.indexOf('&', i + 1);
                if (amphersandIdx != -1 && amphersandIdx < semiColonIdx) {
                    // Then the text looks like &...&...;
                    writer.write(c);
                    continue;
                }
                String entityContent = str.substring(nextIdx, semiColonIdx);
                int entityValue = -1;
                int entityContentLen = entityContent.length();
                if (entityContentLen > 0) {
                    if (entityContent.charAt(0) == '#') { // escaped value content is an integer (decimal or
                        // hexidecimal)
                        if (entityContentLen > 1) {
                            char isHexChar = entityContent.charAt(1);
                            try {
                                switch (isHexChar) {
                                    case 'X' :
                                    case 'x' : {
                                        entityValue = Integer.parseInt(entityContent.substring(2), 16);
                                        break;
                                    }
                                    default : {
                                        entityValue = Integer.parseInt(entityContent.substring(1), 10);
                                    }
                                }
                                if (entityValue > 0xFFFF) {
                                    entityValue = -1;
                                }
                            } catch (NumberFormatException e) {
                                entityValue = -1;
                            }
                        }
                    } else { // escaped value content is an entity name
                        entityValue = this.entityValue(entityContent);
                    }
                }

                if (entityValue == -1) {
                    writer.write('&');
                    writer.write(entityContent);
                    writer.write(';');
                } else {
                    writer.write(entityValue);
                }
                i = semiColonIdx; // move index up to the semi-colon
            } else {
                writer.write(c);
            }
        }
    }

}
