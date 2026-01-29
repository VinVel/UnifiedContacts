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

package net.velcore.unifiedcontacts.domain.edit

import net.velcore.unifiedcontacts.domain.contact.dataitem.DataItem

sealed class Change {
    data class Add(
        val rawContactId: Long,
        val item: DataItem
    ): Change()

    data class Update(
        val itemId: Long,
        val newItem: DataItem
    ): Change()

    data class Delete(
        val itemId: Long
    ): Change()

}