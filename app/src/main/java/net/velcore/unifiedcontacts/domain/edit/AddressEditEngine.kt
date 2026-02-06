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

import net.velcore.unifiedcontacts.domain.contact.DataItem

//This data class represents the fields the user wants to change
data class AddressPatch(
    val street: String? = null,
    val pobox: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val type: Int? = null,
    val label: String? = null,
    val formattedAddress: String? = null
)

//This adds context to the patch
data class AddressEdit(
    val rawContactId: Long,
    val itemId: Long,
    val patch: AddressPatch
)

//Applies this patch to an existing AddressItem.
private fun AddressPatch.applyTo(
    old: DataItem.AddressItem
): DataItem.AddressItem {
    return old.copy(
        street = street ?: old.street,
        pobox = pobox ?: old.pobox,
        neighborhood = neighborhood ?: old.neighborhood,
        city = city ?: old.city,
        region = region ?: old.region,
        postcode = postcode ?: old.postcode,
        country = country ?: old.country,
        type = type ?: old.type,
        label = label ?: old.label,
        formattedAddress = formattedAddress ?: old.formattedAddress
    )
}

//function that should be called when a user wants to change an address
fun AddressEdit.toChange(
    oldItem: DataItem.AddressItem
): Change.Update {
    require(oldItem.id == itemId) {
        "AddressEdit itemId does not match AddressItem id"
    }

    return Change.Update(
        rawContactId = rawContactId,
        oldItemId = itemId,
        newItem = patch.applyTo(oldItem)
    )
}