package com.opensandbox.dhizuku.ui

/** 列表里的一行：应用 + 它当前的状态。 */
data class AppRow(
    val packageName: String,
    val label: String,
    val frozen: Boolean,
    val uninstallBlocked: Boolean
)
