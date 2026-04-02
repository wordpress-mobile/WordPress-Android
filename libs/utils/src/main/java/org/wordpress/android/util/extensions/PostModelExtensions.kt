package org.wordpress.android.util.extensions

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.PublicizeSkipConnection

fun PostModel.updatePublicizeSkipConnections(publicizeSkipConnectionsList: MutableList<PublicizeSkipConnection>) =
    setPublicizeSkipConnectionsJson(Gson().toJson(publicizeSkipConnectionsList))
