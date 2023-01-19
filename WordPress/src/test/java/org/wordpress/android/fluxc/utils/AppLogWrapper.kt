package org.wordpress.android.fluxc.utils

import org.wordpress.android.util.AppLog
import javax.inject.Inject

/** This class is copied from the FluxC repository in order to enable mocking (and verifying) in the dependent test
 *  class. This is because verifying any invocations on the mock of the real class exposes broken linkage due to our
 *  separate test version of the AppLog (i.e. "Cannot access class 'org.wordpress.android.util.AppLog.T'. Check your
 *  module classpath for missing or conflicting dependencies").
 *
 *  This is a somewhat crude solution, but since this wrapper class is so trivial, this seems to be the most
 *  straightforward way to enable log verification for any SUT that depends on this wrapper.
 */
class AppLogWrapper
@Inject constructor() {
    fun d(tag: AppLog.T, message: String) = AppLog.d(tag, message)
    fun e(tag: AppLog.T, message: String) = AppLog.e(tag, message)
    fun w(tag: AppLog.T, message: String) = AppLog.w(tag, message)
}
