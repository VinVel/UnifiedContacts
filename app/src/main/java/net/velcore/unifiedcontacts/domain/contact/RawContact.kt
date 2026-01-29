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

import net.velcore.unifiedcontacts.domain.contact.dataitem.DataItem

data class RawContact(
    val id: Long,
    val account: AccountRef,
    val items: List<DataItem>,
    val origin: RawContactOrigin,
    val writeState: WriteState = WriteState.WRITABLE,
    val sourceId: String? = null,
    val linkId: String? = null
)
