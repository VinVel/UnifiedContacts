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

package net.velcore.unifiedcontacts.data.cache

import net.velcore.unifiedcontacts.domain.contact.AccountRef

object ContactCache {
    private const val CACHE_KEY_ALL_ACCOUNTS = "__ALL_ACCOUNTS__"
    private val lock = Any()
    private val contactNamesByAccount = mutableMapOf<String, List<String>>()

    fun getContactNames(accountRef: AccountRef?): List<String>? {
        val key = cacheKey(accountRef)
        synchronized(lock) {
            return contactNamesByAccount[key]
        }
    }

    fun putContactNames(accountRef: AccountRef?, names: List<String>) {
        val key = cacheKey(accountRef)
        synchronized(lock) {
            contactNamesByAccount[key] = names
        }
    }

    fun clearAll() {
        synchronized(lock) {
            contactNamesByAccount.clear()
        }
    }

    private fun cacheKey(accountRef: AccountRef?): String {
        return accountRef?.let { "${it.name}|${it.type}" } ?: CACHE_KEY_ALL_ACCOUNTS
    }
}