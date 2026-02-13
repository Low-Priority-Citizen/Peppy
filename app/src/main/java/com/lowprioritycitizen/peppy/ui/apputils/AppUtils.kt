package com.lowprioritycitizen.peppy.ui.apputils

import android.content.Context

fun getInstalledApps(context: Context): List<Pair<String, String>> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(0)

    return apps.map {
        val label = pm.getApplicationLabel(it).toString()
        val pkg = it.packageName
        label to pkg
    }.sortedBy { it.first }
}
