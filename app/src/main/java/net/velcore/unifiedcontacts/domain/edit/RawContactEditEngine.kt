/*
 * Copyright (c) 2026 VinVel
 * SPDX-License-Identifier: MPL-2.0
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: unifiedcontacts.velcore.net
 */

package net.velcore.unifiedcontacts.domain.edit

import net.velcore.unifiedcontacts.domain.contact.RawContact
import net.velcore.unifiedcontacts.domain.contact.DataItem

//This is kind of the dumb transformation Engine, it takes a rawContact and returns a new rawContact
object RawContactEditEngine {
    fun apply(raw: RawContact, request: RawContactEditRequest): RawContact {
        var updated = raw
        for (change in request.changes) {
            updated = when (change) {
                is Change.Add -> applyAdd(updated, change)
                is Change.Update -> applyUpdate(updated, change)
                is Change.Delete -> applyDelete(updated,change)
            }
        }
        return updated
    }

    private fun applyAdd(raw: RawContact, change: Change.Add): RawContact {
        return raw.copy(items = raw.items + change.addItem)
    }

    private fun applyUpdate(raw: RawContact, change: Change.Update): RawContact {
        val newItems = mutableListOf<DataItem>()
        for (item in raw.items){
            if (item.id == change.oldItemId) {
                newItems.add(change.newItem)
            }
            else {
                newItems.add(item)
            }
        }
        return raw.copy(items = newItems)
    }

    private fun applyDelete(raw: RawContact, change: Change.Delete): RawContact {
        val newItems = mutableListOf<DataItem>()
        for (item in raw.items) {
            if (item.id != change.itemId) {
                newItems.add(item)
            }
        }
        return raw.copy(items = newItems)
    }
}