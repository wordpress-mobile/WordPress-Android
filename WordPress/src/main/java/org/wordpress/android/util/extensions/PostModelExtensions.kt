package org.wordpress.android.util.extensions

import org.wordpress.android.fluxc.model.PostImmutableModel

fun PostImmutableModel.publicizeSkipConnectionsList(): List<String> =
    publicizeSkipConnections.split(",")
