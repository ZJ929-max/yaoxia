// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.core

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.opensandbox.dhizuku.R
import java.io.File

/**
 * 把"闪退"变成"看得见的错误"：
 * 任何未捕获异常都写入 files/crash-last.txt，并弹窗展示，可一键复制。
 */
object FailureReporter {
    private const val FILE_NAME = "crash-last.txt"

    /** 兜住整个进程的未捕获异常（写盘后仍交回系统，保持原生崩溃行为）。 */
    fun install(activity: Activity) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(activity, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** 可选：把某段操作包起来，出错时弹窗而不是闪退。 */
    fun guard(
        activity: Activity,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (throwable: Throwable) {
            write(activity, throwable)
            show(activity, throwable)
        }
    }

    fun write(
        context: Context,
        throwable: Throwable,
    ) {
        runCatching {
            File(context.filesDir, FILE_NAME).writeText(throwable.stackTraceToString())
        }
    }

    fun show(
        activity: Activity,
        throwable: Throwable,
    ) {
        val report = throwable.stackTraceToString()
        runCatching {
            AlertDialog
                .Builder(activity)
                .setTitle(activity.getString(R.string.crash_title, throwable.javaClass.simpleName))
                .setMessage(report.take(1500))
                .setPositiveButton(R.string.crash_copy) { _, _ ->
                    val clipboard =
                        activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("crash", report))
                    Toast.makeText(activity, R.string.crash_copied, Toast.LENGTH_SHORT).show()
                }.setNegativeButton(R.string.crash_close, null)
                .show()
        }
    }
}
