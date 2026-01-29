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

import net.velcore.unifiedcontacts.domain.contact.Contact
import net.velcore.unifiedcontacts.domain.contact.RawContact
import net.velcore.unifiedcontacts.domain.contact.dataitem.DataItem

//This is kind of the dumb transformation Engine, it takes a rawContact and returns a new rawContact
object EditEngine {
    fun apply(contact: Contact, request: ContactEditRequest): Contact {
        var updated = contact
        for (change in request.changes) {
            updated = when (change) {
                is Change.Add -> applyAdd(updated, change)
                is Change.Update -> applyUpdate(updated, change)
                is Change.Delete -> applyDelete(updated,change)
            }
        }
        return updated
    }

    private fun updateRawContact (
        contact: Contact,
        rawContactId: Long,
        transform: (RawContact) -> RawContact
    ): Contact {
        val newRawContact = mutableListOf<RawContact>()

        for (raw in contact.rawContacts) {
            if (raw.id == rawContactId) {
                newRawContact.add(transform(raw))
            }
            else {
                newRawContact.add(raw)
            }
        }
        return contact.copy(rawContacts = newRawContact)
    }

    private fun applyAdd(contact: Contact, change: Change.Add): Contact {
        return updateRawContact(contact, change.rawContactId) {raw ->
            raw.copy(items = raw.items + change.addItem)
        }
    }

    private fun applyUpdate(contact: Contact, change: Change.Update): Contact {
        return updateRawContact(contact, change.rawContactId) {raw ->
            val newItems = mutableListOf<DataItem>()

            for (item in raw.items) {
                if (item.id == change.oldItemId) {
                    newItems.add(change.newItem)
                }
                else {
                    newItems.add(item)
                }
            }
            raw.copy(items = newItems)
        }
    }

    private fun applyDelete(contact: Contact, change: Change.Delete): Contact {
        return updateRawContact(contact, change.rawContactId) { raw ->
            val newItems = mutableListOf<DataItem>()

            for (item in raw.items) {
                if (item.id != change.itemId) {
                    newItems.add(item)
                }
            }

            raw.copy(items = newItems)
        }
    }
}