package com.opensandbox.dhizuku.apps

/** 一个可被管控的应用：只描述"是谁"，不描述"状态"。 */
data class AppEntry(
    val packageName: String,
    val label: String,
)
