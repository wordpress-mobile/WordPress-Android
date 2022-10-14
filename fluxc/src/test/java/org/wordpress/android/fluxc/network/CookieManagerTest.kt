package org.wordpress.android.fluxc.network

import org.junit.Assert
import org.junit.Test
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.util.LinkedList

/**
 * The test cases here are copied from OpenJdk's CookieManagerTest:
 * https://github.com/openjdk/jdk/blob/20db7800a657b311eeac504a2bbae4adbc209dbf/test/jdk/java/net/CookieHandler/CookieManagerTest.java
 */
class CookieManagerTest {
    private val testCount = 6
    private val localHostAddr = "wordpress.com"

    private val testCases: Array<Array<CookieTestCase>> = Array(testCount) { emptyArray() }
    private val testPolicies: Array<CookiePolicy?> = arrayOfNulls(testCount)

    init {
        var count = 0

        // an http session with Netscape cookies exchanged
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER
        testCases[count++] = arrayOf(
            CookieTestCase(
                "Set-Cookie",
                "CUSTOMER=WILE:BOB; " +
                    "path=/; expires=Sat, 09-Nov-2030 23:12:40 GMT;" + "domain=." +
                    localHostAddr,
                "CUSTOMER=WILE:BOB",
                "/"
            ),
            CookieTestCase(
                "Set-Cookie",
                "PART_NUMBER=ROCKET_LAUNCHER_0001; path=/;domain=.$localHostAddr",
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
            ),
            CookieTestCase(
                "Set-Cookie",
                "SHIPPING=FEDEX; path=/foo;domain=.$localHostAddr",
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
            ),
            CookieTestCase(
                "Set-Cookie",
                "SHIPPING=FEDEX; path=/foo;domain=.$localHostAddr",
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001; SHIPPING=FEDEX",
                "/foo"
            )
        )

        // check whether or not path rule is applied
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER
        testCases[count++] = arrayOf(
            CookieTestCase(
                "Set-Cookie",
                "PART_NUMBER=ROCKET_LAUNCHER_0001; path=/;domain=.$localHostAddr",
                "PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
            ),
            CookieTestCase(
                "Set-Cookie",
                "PART_NUMBER=RIDING_ROCKET_0023; path=/ammo;domain=.$localHostAddr",
                "PART_NUMBER=RIDING_ROCKET_0023; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/ammo"
            )
        )

        // an http session with rfc2965 cookies exchanged
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER
        testCases[count++] = arrayOf(
            CookieTestCase(
                "Set-Cookie2",
                "Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\";domain=.$localHostAddr",
                "\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";\$Domain=\".$localHostAddr\"",
                "/acme/login"
            ),
            CookieTestCase(
                "Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\";Path=\"/acme\";domain=.$localHostAddr",
                ("\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";" + "\$Domain=\"." +
                    localHostAddr + "\"" + "; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";"
                    + "\$Domain=\"." + localHostAddr + "\""),
                "/acme/pickitem"
            ),
            CookieTestCase(
                "Set-Cookie2",
                "Shipping=\"FedEx\"; Version=\"1\"; Path=\"/acme\";domain=.$localHostAddr",
                ("\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";" + "\$Domain=\"." + localHostAddr +
                    "\"" + "; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";" + "\$Domain=\"."
                    + localHostAddr + "\"" + "; Shipping=\"FedEx\";\$Path=\"/acme\";" +
                    "\$Domain=\"." + localHostAddr + "\""),
                "/acme/shipping"
            )
        )

        // check whether or not the path rule is applied
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER
        testCases[count++] = arrayOf(
            CookieTestCase(
                "Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\"; Path=\"/acme\";domain=.$localHostAddr",
                "\$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";\$Domain=\".$localHostAddr\"",
                "/acme/ammo"
            ),
            CookieTestCase(
                "Set-Cookie2",
                ("Part_Number=\"Riding_Rocket_0023\"; Version=\"1\"; Path=\"/acme/ammo\";" + "domain=."
                    + localHostAddr),
                ("\$Version=\"1\"; Part_Number=\"Riding_Rocket_0023\";\$Path=\"/acme/ammo\";\$Domain=\"."
                    + localHostAddr + "\"" + "; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";"
                    + "\$Domain=\"." + localHostAddr + "\""),
                "/acme/ammo"
            ),
            CookieTestCase(
                "",
                "",
                "\$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";\$Domain=\".$localHostAddr\"",
                "/acme/parts"
            )
        )

        // new cookie should overwrite old cookie
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER
        testCases[count++] = arrayOf(
            CookieTestCase(
                "Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\"; Path=\"/acme\";domain=.$localHostAddr",
                "\$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";\$Domain=\".$localHostAddr\"",
                "/acme"
            ),
            CookieTestCase(
                "Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_2000\"; Version=\"1\"; Path=\"/acme\";domain=.$localHostAddr",
                "\$Version=\"1\"; Part_Number=\"Rocket_Launcher_2000\";\$Path=\"/acme\";\$Domain=\".$localHostAddr\"",
                "/acme"
            )
        )

        // cookies without domain attributes
        // RFC 2965 states that domain should default to host
        testPolicies[count] = CookiePolicy.ACCEPT_ALL
        testCases[count] = arrayOf(
            CookieTestCase(
                "Set-Cookie2",
                "Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\"",
                "\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";\$Domain=\"$localHostAddr\"",
                "/acme/login"
            ),
            CookieTestCase(
                "Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\";Path=\"/acme\"",
                ("\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";\$Domain=\"" + localHostAddr + "\"" +
                    "; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";\$Domain=\"" + localHostAddr + "\""),
                "/acme/pickitem"
            ),
            CookieTestCase(
                "Set-Cookie2",
                "Shipping=\"FedEx\"; Version=\"1\"; Path=\"/acme\"",
                ("\$Version=\"1\"; Customer=\"WILE_E_COYOTE\";\$Path=\"/acme\";\$Domain=\"" + localHostAddr + "\"" +
                    "; Part_Number=\"Rocket_Launcher_0001\";\$Path=\"/acme\";\$Domain=\"" + localHostAddr + "\"" +
                    "; Shipping=\"FedEx\";\$Path=\"/acme\";\$Domain=\"" + localHostAddr + "\""),
                "/acme/shipping"
            )
        )
    }

    @Test
    fun test_cookies() {
        val cookieManager: CookieManager = PatchedCookieManager()
        for (testCases in testCases) {
            for (testCase in testCases) {
                val path = URI("https://$localHostAddr${testCase.serverPath}")

                cookieManager.put(
                    path,
                    mapOf(testCase.headerToken to listOf(testCase.cookieToSend))
                )
                val cookieManagerCookies = cookieManager.get(path, emptyMap())["Cookie"]
                    ?.joinToString(";")
                    .orEmpty()

                Assert.assertTrue(
                    "Cookies not matching for testCase: $testCase",
                    cookieEquals(testCase.cookieToRecv, cookieManagerCookies)
                )
            }
            cookieManager.cookieStore.removeAll()
        }
    }

    private fun cookieEquals(s1: String, s2: String): Boolean {
        val s1a = s1.removeWhiteSpace().split(";").toTypedArray()
        val s2a = s2.removeWhiteSpace().split(";").toTypedArray()
        val l1: List<String> = LinkedList(listOf(*s1a)).sorted()
        val l2: List<String> = LinkedList(listOf(*s2a)).sorted()
        for ((i, s) in l1.withIndex()) {
            if (s != l2[i]) {
                return false
            }
        }
        return true
    }

    private fun String.removeWhiteSpace(): String {
        val sb = StringBuilder()
        for (i in indices) {
            val c = get(i)
            if (!Character.isWhitespace(c)) sb.append(c)
        }
        return sb.toString()
    }
}

data class CookieTestCase(
    val headerToken: String,
    val cookieToSend: String,
    val cookieToRecv: String,
    val serverPath: String
)
