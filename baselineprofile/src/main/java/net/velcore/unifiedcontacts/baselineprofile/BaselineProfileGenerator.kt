/*
 * Copyright (c) 2026 VinVel
 * SPDX-License-Identifier: MPL-2.0
 * Incompatible With Secondary Licenses.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: unifiedcontacts.velcore.net
 */

package net.velcore.unifiedcontacts.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "net.velcore.unifiedcontacts",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            grantContactsPermissionIfShown()
            device.waitForIdle()

            val width = device.displayWidth
            val height = device.displayHeight
            val centerX = width / 2
            val centerY = height / 2
            val indexX = (width * 0.96f).toInt()

            // Warm typical contact list fling path.
            repeat(4) {
                device.swipe(centerX, (height * 0.78f).toInt(), centerX, (height * 0.22f).toInt(), 24)
                device.swipe(centerX, (height * 0.22f).toInt(), centerX, (height * 0.78f).toInt(), 24)
            }

            // Warm sidebar drag path across the alphabet index.
            repeat(3) {
                device.swipe(indexX, (height * 0.78f).toInt(), indexX, (height * 0.22f).toInt(), 24)
                device.swipe(indexX, (height * 0.22f).toInt(), indexX, (height * 0.78f).toInt(), 24)
            }

            // Warm item realization around center once more.
            device.swipe(centerX, (centerY * 1.4f).toInt(), centerX, (centerY * 0.6f).toInt(), 18)
        }
    }

    private fun MacrobenchmarkScope.grantContactsPermissionIfShown() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val maybeDialog = device.wait(
            Until.findObject(By.res("com.android.permissioncontroller", "permission_message")),
            2_000,
        )
        if (maybeDialog != null) {
            val allowButton =
                device.findObject(By.res("com.android.permissioncontroller", "permission_allow_button")) ?:
                device.findObject(By.res("com.android.permissioncontroller", "permission_allow_foreground_only_button")) ?:
                device.findObject(By.res("com.android.permissioncontroller", "permission_allow_one_time_button"))
            allowButton?.click()
            device.waitForIdle()
        }
    }
}
