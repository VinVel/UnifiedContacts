/*
 * Copyright (c) 2026 VinVel
 * SPDX-License-Identifier: MPL-2.0
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://unifiedcontacts.velcore.net/
 */

package net.velcore.unifiedcontacts.domain.contact

//aggregation of different RawContacts
data class Contact(
    val id: Long,
    val rawContacts: List<RawContact>
) {
    //whether or not only one syncadapter is chosen
    fun filtered(accountTypes: Set<String>): Contact =
        copy(rawContacts = rawContacts.filter { it.account.type in accountTypes })

    //whether the RawContact is even writable
    fun writableRawContacts(): List<RawContact> =
        rawContacts.filter { it.writeState == WriteState.WRITABLE }

    //if multiple similar rawContacts with different syncadapters exist which one to chose.
    fun editTargets(): List<RawContact> =
        writableRawContacts()
}