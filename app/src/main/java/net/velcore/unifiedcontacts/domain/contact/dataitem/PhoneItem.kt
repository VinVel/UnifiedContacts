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

data class PhoneItem(
    override val id: Long,
    val number: String,
    val type: Int, //used to categorise what type of phone number it is, e.g. Home, Mobile, Work, etc.
    val label: String?

): DataItem() {
    override val mimeType: String = MimeTypes.PHONE
}
