// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 唯一的线程边界：耗时工作都在单线程池里跑，结果统一回到主线程。
 * 调用方（界面）不需要知道 Executor / Handler 的存在。
 */
class AsyncTaskRunner(
    threadName: String,
) {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, threadName).apply { isDaemon = true }
        }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun <T> execute(
        work: () -> Outcome<T>,
        onResult: (Outcome<T>) -> Unit,
    ) {
        executor.execute {
            val result =
                try {
                    work()
                } catch (throwable: Throwable) {
                    Outcome.fail<T>(throwable)
                }
            mainHandler.post { onResult(result) }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
