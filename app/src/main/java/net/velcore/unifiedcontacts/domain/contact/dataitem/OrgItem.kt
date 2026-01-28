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

data class OrgItem(
    override val id: Long,
    val organisation: String?,
    val division: String?,
    val title: String?

): DataItem() {
    override val mimeType = MimeTypes.ORGANIZATION
}
