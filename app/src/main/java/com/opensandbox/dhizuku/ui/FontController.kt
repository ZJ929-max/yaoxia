// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.core.AsyncTaskRunner

/**
 * 字体选择与下载的唯一入口。
 * 从 AppState 拆出来：它只关心「用哪套字、字体文件在不在」，与业务状态无关。
 */
class FontController(
    private val context: Context,
    private val async: AsyncTaskRunner,
    private val onStatus: (String) -> Unit,
    private val onChanged: () -> Unit,
) {
    var choice by androidx.compose.runtime.mutableStateOf(FontPrefs.load(context))
        private set

    var harmonyReady by androidx.compose.runtime.mutableStateOf(
        FontFiles.isReady(context, DownloadableFont.HARMONY),
    )
        private set

    var notoReady by androidx.compose.runtime.mutableStateOf(
        FontFiles.isReady(context, DownloadableFont.NOTO),
    )
        private set

    var busy by androidx.compose.runtime.mutableStateOf(false)
        private set

    fun choose(font: AppFont) {
        choice = font
        FontPrefs.save(context, font)
        onChanged()
    }

    fun download(which: DownloadableFont) {
        if (busy) return
        busy = true
        onStatus("正在下载${which.label}…")
        async.execute({ FontFiles.download(context, which) }, { outcome ->
            busy = false
            harmonyReady = FontFiles.isReady(context, DownloadableFont.HARMONY)
            notoReady = FontFiles.isReady(context, DownloadableFont.NOTO)
            if (outcome.isSuccess) {
                choose(if (which == DownloadableFont.HARMONY) AppFont.HARMONY else AppFont.DEFAULT)
                onStatus("${which.label} 已就绪")
            } else {
                onStatus("${which.label} 下载失败：${outcome.errorOrNull()}")
            }
        })
    }

    fun downloadHarmony() = download(DownloadableFont.HARMONY)
}
