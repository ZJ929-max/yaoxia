// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku

import android.app.admin.DeviceAdminReceiver

/**
 * 只是个"身份证"：DevicePolicyManager 多数方法都需要一个 ComponentName 参数，
 * 被委派的应用也要靠它来标识自己。不需要用户去设置里手动激活。
 */
class SandboxAdmin : DeviceAdminReceiver()
