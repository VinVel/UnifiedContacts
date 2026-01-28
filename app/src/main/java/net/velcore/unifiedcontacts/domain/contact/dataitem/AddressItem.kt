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

package net.velcore.unifiedcontacts.domain.contact.dataitem

import net.velcore.unifiedcontacts.domain.util.MimeTypes

data class AddressItem(
    override val id: Long,
    val street: String?,
    val pobox: String?,
    val neighborhood: String?,
    val city: String?,
    val region: String?,
    val postcode: String?,
    val country: String?,
    val type: Int,              //e.g. if the address is for home, work or others
    val label: String?,
    val formattedAddress: String?

): DataItem() {
    override val mimeType = MimeTypes.POSTAL_ADDRESS
}
