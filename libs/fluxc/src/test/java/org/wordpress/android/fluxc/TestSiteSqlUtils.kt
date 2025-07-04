package org.wordpress.android.fluxc

import org.wordpress.android.fluxc.encryption.EncryptionUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils

object TestSiteSqlUtils {
    val siteSqlUtils = SiteSqlUtils(EncryptionUtils())
}
